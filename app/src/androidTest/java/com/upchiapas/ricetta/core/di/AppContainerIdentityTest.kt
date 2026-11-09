package com.upchiapas.ricetta.core.di

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.upchiapas.ricetta.RicettaApplication
import com.upchiapas.ricetta.core.network.AuthInterceptor
import com.upchiapas.ricetta.presentation.RicettaApp
import com.upchiapas.ricetta.presentation.theme.RicettaTheme
import okhttp3.OkHttpClient
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit

@RunWith(AndroidJUnit4::class)
class AppContainerIdentityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun applicationOwnsOneContainerAndSharesItsDependencies() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as RicettaApplication

        val container = application.appContainer
        assertSame(container, application.appContainer)
        assertSame(container.sessionStorage, container.sessionStorage)
        assertSame(container.authRepository, container.authRepository)
        assertSame(container.recipeRepository, container.recipeRepository)

        // Estos objetos son privados en producción; invocar sus getters privados aquí verifica
        // la identidad lazy sin ampliar la API pública del contenedor.
        val client = privateLazyValue<OkHttpClient>(container, "okHttpClient")
        val retrofit = privateLazyValue<Retrofit>(container, "retrofit")
        assertSame(client, privateLazyValue(container, "okHttpClient"))
        assertSame(retrofit, privateLazyValue(container, "retrofit"))
        assertSame(client, retrofit.callFactory())

        val authInterceptor = client.interceptors.filterIsInstance<AuthInterceptor>().single()
        val storageField = AuthInterceptor::class.java.getDeclaredField("sessionStorage")
            .apply { isAccessible = true }
        assertSame(container.sessionStorage, storageField.get(authInterceptor))
    }

    @Test fun productionNavigationContinuesUsingTheApplicationContainerGraph() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as RicettaApplication
        val container = application.appContainer
        compose.setContent {
            RicettaTheme {
                RicettaApp(container.authRepository, container.sessionStorage, container.recipeRepository)
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("login_screen").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("login_register").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("register_screen").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("register_back").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("login_screen").fetchSemanticsNodes().isNotEmpty()
        }

        assertSame(container, application.appContainer)
        assertSame(container.authRepository, application.appContainer.authRepository)
        assertSame(container.sessionStorage, application.appContainer.sessionStorage)
        assertSame(container.recipeRepository, application.appContainer.recipeRepository)
    }

    private fun <T : Any> privateLazyValue(instance: Any, property: String): T {
        val suffix = property.replaceFirstChar { it.uppercase() }
        val getter = instance.javaClass.getDeclaredMethod("get$suffix").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return getter.invoke(instance) as T
    }
}
