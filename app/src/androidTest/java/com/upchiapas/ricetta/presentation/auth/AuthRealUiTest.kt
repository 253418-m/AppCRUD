package com.upchiapas.ricetta.presentation.auth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.upchiapas.ricetta.core.network.AuthInterceptor
import com.upchiapas.ricetta.data.remote.AuthApi
import com.upchiapas.ricetta.data.repository.AuthRepositoryImpl
import com.upchiapas.ricetta.data.session.SharedPreferencesSessionStorage
import com.upchiapas.ricetta.presentation.RicettaApp
import com.upchiapas.ricetta.presentation.theme.RicettaTheme
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Solo se ejecuta con FastAPI real y SQLite aislado proporcionados por el script de verificación. */
@RunWith(AndroidJUnit4::class)
class AuthRealUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun registerLoginAndConfirmedLogoutUseRealApiAndPersistentStorage() {
        val configuredUrl = InstrumentationRegistry.getArguments().getString("fastApiBaseUrl")
        assumeTrue("Requiere fastApiBaseUrl y FastAPI real con base aislada", !configuredUrl.isNullOrBlank())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferencesName = "auth-real-ui-${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val storage = SharedPreferencesSessionStorage(preferences)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(storage))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(requireNotNull(configuredUrl))
            .client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }
                .asConverterFactory("application/json".toMediaType()))
            .build()
        val repository = AuthRepositoryImpl(retrofit.create(AuthApi::class.java), storage)
        val email = "ui-${UUID.randomUUID()}@example.com"
        val password = "SoloPruebasUi123!"

        try {
            compose.setContent { RicettaTheme { RicettaApp(repository, storage) } }
            waitFor("login_screen")
            compose.onNodeWithTag("login_register").performScrollTo().performClick()
            waitFor("register_screen")
            input("register_name", "Prueba UI real")
            input("register_email", email)
            input("register_password", password)
            input("register_confirm", password)
            compose.onNodeWithTag("register_submit").performScrollTo().performClick()
            waitFor("home_provisional")
            assertTrue(runBlocking { storage.hasSession() })
            val reopened = SharedPreferencesSessionStorage(
                context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            )
            assertTrue(runBlocking { reopened.hasSession() })
            // No comparar tokens con assertEquals: un fallo podría imprimirlos.
            assertTrue(storage.getToken() == reopened.getToken())
            compose.onNodeWithTag("login_screen").assertDoesNotExist()
            compose.onNodeWithTag("register_screen").assertDoesNotExist()

            compose.onNodeWithTag("home_logout").performClick()
            compose.onNodeWithTag("logout_dialog").assertIsDisplayed()
            compose.onNodeWithTag("logout_cancel").performClick()
            compose.onNodeWithTag("home_provisional").assertIsDisplayed()
            assertTrue(runBlocking { storage.hasSession() })
            compose.onNodeWithTag("home_logout").performClick()
            compose.onNodeWithTag("logout_confirm").performClick()
            waitFor("login_screen")
            assertFalse(runBlocking { storage.hasSession() })

            input("login_email", email)
            input("login_password", password)
            compose.onNodeWithTag("login_submit").performScrollTo().performClick()
            waitFor("home_provisional")
            assertTrue(runBlocking { storage.hasSession() })
            compose.onNodeWithTag("home_logout").performClick()
            compose.onNodeWithTag("logout_confirm").performClick()
            waitFor("login_screen")
            assertFalse(runBlocking { reopened.hasSession() })
            compose.onNodeWithTag("home_provisional").assertDoesNotExist()
        } finally {
            runBlocking { storage.clearToken() }
            context.deleteSharedPreferences(preferencesName)
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun input(tag: String, value: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextInput(value)
    }

    private fun waitFor(tag: String) {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }
}
