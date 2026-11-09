package com.upchiapas.ricetta.presentation.auth

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.upchiapas.ricetta.domain.model.Usuario
import com.upchiapas.ricetta.domain.model.Receta
import com.upchiapas.ricetta.domain.model.GuardarReceta
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.repository.RecipeRepository
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.domain.session.SessionStorage
import com.upchiapas.ricetta.presentation.RicettaApp
import com.upchiapas.ricetta.presentation.theme.RicettaTheme
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas de UI con dependencias falsas; no son evidencia de conexión a FastAPI. */
@RunWith(AndroidJUnit4::class)
class AuthUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun launch(storage: UiSessionStorage = UiSessionStorage(), repository: UiAuthRepository = UiAuthRepository(storage)) {
        compose.setContent { RicettaTheme { RicettaApp(repository, storage, UiRecipeRepository()) } }
    }

    private fun waitFor(tag: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun input(tag: String, value: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextInput(value)
    }

    private fun loginFields() {
        input("login_email", "ana@example.com")
        input("login_password", "SoloPruebas123!")
    }

    private fun registerFields() {
        input("register_name", "Ana de prueba")
        input("register_email", "ana@example.com")
        input("register_password", "SoloPruebas123!")
        input("register_confirm", "SoloPruebas123!")
    }

    @Test fun loginAndRegisterRenderAndTheirLinksNavigate() {
        launch()
        waitFor("login_screen")
        compose.onNodeWithText("Iniciar sesión").assertExists()
        compose.onNodeWithTag("login_register").performScrollTo().performClick()
        waitFor("register_screen")
        compose.onNodeWithTag("register_name").assertExists()
        compose.onNodeWithTag("register_confirm").assertExists()
        compose.onNodeWithTag("register_back").performClick()
        waitFor("login_screen")
        compose.onNodeWithTag("login_register").performScrollTo().performClick()
        waitFor("register_screen")
        compose.onNodeWithTag("register_login").performScrollTo().performClick()
        waitFor("login_screen")
        compose.onNodeWithTag("recipe_home").assertDoesNotExist()
    }

    @Test fun emptyPasswordFieldsHaveNoPlaceholderAndVisibilityDoesNotInsertText() {
        launch()
        waitFor("login_screen")
        compose.onNodeWithText("••••••••").assertDoesNotExist()
        compose.onNodeWithTag("login_logo").assertExists()
        compose.onNodeWithTag("login_toggle_password").performScrollTo().performClick()
        assertEquals(
            "",
            compose.onNodeWithTag("login_password").fetchSemanticsNode()
                .config[SemanticsProperties.EditableText].text
        )
        compose.onNodeWithTag("login_register").performScrollTo().performClick()
        waitFor("register_screen")
        compose.onNodeWithText("••••••••").assertDoesNotExist()
        compose.onNodeWithTag("register_toggle_password").performScrollTo().performClick()
        compose.onNodeWithTag("register_toggle_confirm").performScrollTo().performClick()
        assertEquals(
            "",
            compose.onNodeWithTag("register_password").fetchSemanticsNode()
                .config[SemanticsProperties.EditableText].text
        )
        assertEquals(
            "",
            compose.onNodeWithTag("register_confirm").fetchSemanticsNode()
                .config[SemanticsProperties.EditableText].text
        )
        compose.onNodeWithText("Mínimo 8 caracteres").assertExists()
    }

    @Test fun invalidFormsShowFieldErrorsWithoutSendingRequests() {
        val storage = UiSessionStorage()
        val repository = UiAuthRepository(storage)
        launch(storage, repository)
        waitFor("login_screen")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa tu correo electrónico.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ingresa tu contraseña.").performScrollTo().assertIsDisplayed()
        input("login_email", "correo-invalido")
        input("login_password", "SoloPruebas123!")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa un correo electrónico válido.").performScrollTo().assertIsDisplayed()
        assertEquals(0, repository.loginCalls.get())
        compose.onNodeWithTag("login_register").performScrollTo().performClick()
        waitFor("register_screen")
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa tu nombre completo.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Confirma tu contraseña.").performScrollTo().assertIsDisplayed()
        assertEquals(0, repository.registerCalls.get())
    }

    private fun assertRenderedPassword(tag: String, visible: Boolean) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag).performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        assertTrue("El campo debe proporcionar su texto renderizado", results.isNotEmpty())
        val expected = "SoloPruebas123!"
        assertTrue("La transformación visual de contraseña no corresponde al control", results.any {
            val rendered = it.layoutInput.text.text
            if (visible) rendered == expected
            else rendered.length == expected.length && rendered != expected &&
                rendered.none { character -> character.isLetterOrDigit() }
        })
    }

    @Test fun passwordVisibilityControlsWorkIndependently() {
        launch()
        waitFor("login_screen")
        input("login_password", "SoloPruebas123!")
        assertRenderedPassword("login_password", visible = false)
        compose.onNodeWithTag("login_toggle_password").performScrollTo().performClick()
        assertRenderedPassword("login_password", visible = true)
        compose.onNodeWithTag("login_toggle_password").performClick()
        assertRenderedPassword("login_password", visible = false)
        compose.onNodeWithTag("login_register").performScrollTo().performClick()
        waitFor("register_screen")
        input("register_password", "SoloPruebas123!")
        input("register_confirm", "SoloPruebas123!")
        assertRenderedPassword("register_password", visible = false)
        assertRenderedPassword("register_confirm", visible = false)
        compose.onNodeWithTag("register_toggle_password").performScrollTo().performClick()
        assertRenderedPassword("register_password", visible = true)
        assertRenderedPassword("register_confirm", visible = false)
        compose.onNodeWithTag("register_toggle_confirm").performScrollTo().performClick()
        assertRenderedPassword("register_password", visible = true)
        assertRenderedPassword("register_confirm", visible = true)
        compose.onNodeWithTag("register_toggle_password").performScrollTo().performClick()
        assertRenderedPassword("register_password", visible = false)
        assertRenderedPassword("register_confirm", visible = true)
    }

    private fun attachRecreationHost(
        activity: ComponentActivity,
        storage: UiSessionStorage,
        repository: UiAuthRepository,
    ) {
        // Usar el mismo callsite de composición antes/después de recreate conserva las
        // claves de rememberSaveable, igual que setContent dentro de onCreate en producción.
        activity.setContent { RicettaTheme { RicettaApp(repository, storage, UiRecipeRepository()) } }
    }

    private fun recreateTestHost(storage: UiSessionStorage, repository: UiAuthRepository) {
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { activity ->
            attachRecreationHost(activity, storage, repository)
        }
    }

    private fun assertInputRetained(tag: String, expected: String) {
        val field = compose.onNodeWithTag(tag).performScrollTo().fetchSemanticsNode()
        assertTrue("El campo debe conservar su estado al recrear la Activity",
            field.config[SemanticsProperties.EditableText].text == expected)
    }

    private fun assertPasswordRetained(fieldTag: String, toggleTag: String) {
        assertRenderedPassword(fieldTag, visible = false)
        compose.onNodeWithTag(toggleTag).performScrollTo().performClick()
        assertRenderedPassword(fieldTag, visible = true)
        compose.onNodeWithTag(toggleTag).performClick()
        assertRenderedPassword(fieldTag, visible = false)
    }

    @Test fun activityRecreationPreservesLoginAndRegistrationRouteAndForm() {
        val storage = UiSessionStorage()
        val repository = UiAuthRepository(storage)
        compose.activityRule.scenario.onActivity { activity ->
            attachRecreationHost(activity, storage, repository)
        }
        waitFor("login_screen")
        loginFields()
        recreateTestHost(storage, repository)
        waitFor("login_screen")
        assertInputRetained("login_email", "ana@example.com")
        assertPasswordRetained("login_password", "login_toggle_password")
        compose.onNodeWithTag("login_register").performScrollTo().performClick()
        waitFor("register_screen")
        registerFields()
        recreateTestHost(storage, repository)
        waitFor("register_screen")
        compose.onNodeWithTag("login_screen").assertDoesNotExist()
        assertInputRetained("register_name", "Ana de prueba")
        assertInputRetained("register_email", "ana@example.com")
        assertPasswordRetained("register_password", "register_toggle_password")
        assertPasswordRetained("register_confirm", "register_toggle_confirm")
        assertEquals(0, repository.loginCalls.get())
        assertEquals(0, repository.registerCalls.get())
    }

    @Test fun loginLogoutConfirmationAndBackStackAreFunctional() {
        val storage = UiSessionStorage()
        val repository = UiAuthRepository(storage)
        launch(storage, repository)
        waitFor("login_screen")
        loginFields()
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        waitFor("recipe_home")
        assertTrue(storage.active)
        compose.onNodeWithTag("login_screen").assertDoesNotExist()
        compose.onNodeWithTag("recipe_logout").performClick()
        compose.onNodeWithTag("logout_dialog").assertIsDisplayed()
        compose.onNodeWithText("¿Cerrar sesión?").assertIsDisplayed()
        compose.onNodeWithTag("logout_cancel").performClick()
        compose.onNodeWithTag("logout_dialog").assertDoesNotExist()
        compose.onNodeWithTag("recipe_home").assertIsDisplayed()
        assertEquals(0, repository.logoutCalls.get())
        assertTrue(storage.active)
        compose.onNodeWithTag("recipe_logout").performClick()
        compose.onNodeWithTag("logout_confirm").performClick()
        waitFor("login_screen")
        assertFalse(storage.active)
        assertEquals(1, repository.logoutCalls.get())
        compose.onNodeWithTag("recipe_home").assertDoesNotExist()
        Espresso.pressBackUnconditionally()
        if (compose.activityRule.scenario.state != Lifecycle.State.DESTROYED) {
            compose.onNodeWithTag("recipe_home").assertDoesNotExist()
        }
    }

    @Test fun storedSessionRestoresProtectedDestinationWithoutLogin() {
        val storage = UiSessionStorage(active = true)
        launch(storage)
        waitFor("recipe_home")
        compose.onNodeWithTag("login_screen").assertDoesNotExist()
        compose.onNodeWithTag("register_screen").assertDoesNotExist()
    }

    @Test fun authenticationAndConflictErrorsAreVisibleAndRecoverable() {
        val storage = UiSessionStorage()
        val repository = UiAuthRepository(storage).apply {
            loginResult = AppResult.Failure(DataError.Unauthorized)
            registerResult = AppResult.Failure(DataError.Conflict)
        }
        launch(storage, repository)
        waitFor("login_screen")
        loginFields()
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.onNodeWithText("El correo o la contraseña son incorrectos.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("login_register").performScrollTo().performClick()
        waitFor("register_screen")
        registerFields()
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        compose.onNodeWithText("Este correo ya está registrado").performScrollTo().assertIsDisplayed()
        assertFalse(storage.active)
        repository.registerResult = AppResult.Success(UiAuthRepository.user)
        compose.onNodeWithTag("register_email").performScrollTo().performTextReplacement("otra@example.com")
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        waitFor("recipe_home")
        assertTrue(storage.active)
    }

    @Test fun loadingDisablesTheSubmitButtonUntilAuthenticationCompletes() {
        val storage = UiSessionStorage()
        val response = CompletableDeferred<AppResult<Usuario>>()
        val repository = UiAuthRepository(storage).apply { pendingLogin = response }
        launch(storage, repository)
        waitFor("login_screen")
        loginFields()
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { repository.loginCalls.get() == 1 }
        compose.onNodeWithTag("login_submit").assertIsNotEnabled()
        assertEquals(1, repository.loginCalls.get())
        response.complete(AppResult.Success(UiAuthRepository.user))
        waitFor("recipe_home")
    }
}

private class UiSessionStorage(@Volatile var active: Boolean = false) : SessionStorage {
    override fun getToken(): String? = if (active) "token-ficticio-ui" else null
    override suspend fun hasSession(): Boolean = active
    override suspend fun saveToken(token: String) { active = true }
    override suspend fun clearToken() { active = false }
}

private class UiAuthRepository(private val storage: UiSessionStorage) : AuthRepository {
    val loginCalls = AtomicInteger()
    val registerCalls = AtomicInteger()
    val logoutCalls = AtomicInteger()
    var loginResult: AppResult<Usuario> = AppResult.Success(user)
    var registerResult: AppResult<Usuario> = AppResult.Success(user)
    var pendingLogin: CompletableDeferred<AppResult<Usuario>>? = null

    override suspend fun iniciarSesion(correo: String, password: String): AppResult<Usuario> {
        loginCalls.incrementAndGet()
        return (pendingLogin?.await() ?: loginResult).also {
            if (it is AppResult.Success) storage.saveToken("token-ficticio-ui")
        }
    }
    override suspend fun registrar(nombre: String, correo: String, password: String): AppResult<Usuario> {
        registerCalls.incrementAndGet()
        return registerResult.also {
            if (it is AppResult.Success) storage.saveToken("token-ficticio-ui")
        }
    }
    override suspend fun cerrarSesion(): AppResult<Unit> {
        logoutCalls.incrementAndGet()
        storage.clearToken()
        return AppResult.Success(Unit)
    }

    companion object { val user = Usuario(1, "Ana", "ana@example.com") }
}

private class UiRecipeRepository : RecipeRepository {
    override suspend fun obtenerRecetas(): AppResult<List<Receta>> = AppResult.Success(emptyList())
    override suspend fun obtenerReceta(id: Int): AppResult<Receta> = AppResult.Failure(DataError.NotFound)
    override suspend fun crearReceta(receta: GuardarReceta): AppResult<Receta> = error("No se usa en autenticación")
    override suspend fun actualizarReceta(id: Int, receta: GuardarReceta): AppResult<Receta> = error("No se usa en autenticación")
    override suspend fun eliminarReceta(id: Int): AppResult<Unit> = error("No se usa en autenticación")
}
