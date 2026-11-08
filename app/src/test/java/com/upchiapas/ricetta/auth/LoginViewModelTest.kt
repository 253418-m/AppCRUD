package com.upchiapas.ricetta.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.upchiapas.ricetta.domain.model.Usuario
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.presentation.auth.AuthViewModelFactories
import com.upchiapas.ricetta.presentation.auth.login.LoginViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun emptyFieldsDoNotCallRepository() = runTest {
        val repository = FakeAuthRepository()
        val vm = LoginViewModel(repository)
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.correoError)
        assertNotNull(vm.state.value.passwordError)
        assertEquals(0, repository.loginCalls)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun invalidEmailIsAFieldErrorAndDoesNotSend() = runTest {
        val repository = FakeAuthRepository()
        val vm = LoginViewModel(repository)
        vm.onCorreoChange("correo-sin-arroba")
        vm.onPasswordChange("ClavePrueba123")
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.correoError)
        assertNull(vm.state.value.passwordError)
        assertEquals(0, repository.loginCalls)
    }

    @Test fun successfulLoginUsesRepositoryAndClearsPassword() = runTest {
        val repository = FakeAuthRepository()
        val vm = LoginViewModel(repository)
        vm.onCorreoChange(" ana@example.com ")
        vm.onPasswordChange(" ClavePrueba123 ")
        vm.submit()
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertEquals("ana@example.com" to " ClavePrueba123 ", repository.lastLogin)
        assertEquals(testUser, vm.state.value.authenticatedUser)
        assertEquals("", vm.state.value.password)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.errorMessage)
    }

    @Test fun unauthorizedPreservesFormAndAllowsCorrectionAndRetry() = runTest {
        val repository = FakeAuthRepository().apply {
            login = { AppResult.Failure(DataError.Unauthorized) }
        }
        val vm = LoginViewModel(repository)
        vm.onCorreoChange("ana@example.com")
        vm.onPasswordChange("ContraseñaIncorrecta")
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.errorMessage)
        assertEquals("ana@example.com", vm.state.value.correo)
        assertEquals("ContraseñaIncorrecta", vm.state.value.password)
        assertNull(vm.state.value.authenticatedUser)
        repository.login = { AppResult.Success(testUser) }
        vm.onPasswordChange("Correcta123")
        vm.submit()
        advanceUntilIdle()
        assertEquals(testUser, vm.state.value.authenticatedUser)
        assertEquals(2, repository.loginCalls)
    }

    @Test fun networkTimeoutAndServerFailuresRemainRecoverable() = runTest {
        for (error in listOf(DataError.NoInternet, DataError.Timeout, DataError.Server,
            DataError.Validation, DataError.Storage, DataError.Serialization, DataError.Unknown)) {
            val repository = FakeAuthRepository().apply { login = { AppResult.Failure(error) } }
            val vm = LoginViewModel(repository)
            vm.onCorreoChange("ana@example.com")
            vm.onPasswordChange("ClavePrueba123")
            vm.submit()
            advanceUntilIdle()
            assertNotNull(error.name, vm.state.value.errorMessage)
            assertEquals("ClavePrueba123", vm.state.value.password)
            assertFalse(vm.state.value.isLoading)
            assertNull(vm.state.value.authenticatedUser)
        }
    }

    @Test fun loadingPreventsDuplicateRequests() = runTest {
        val response = CompletableDeferred<AppResult<Usuario>>()
        val repository = FakeAuthRepository().apply { login = { response.await() } }
        val vm = LoginViewModel(repository)
        vm.onCorreoChange("ana@example.com")
        vm.onPasswordChange("ClavePrueba123")
        vm.submit()
        vm.submit()
        runCurrent()
        vm.submit()
        assertTrue(vm.state.value.isLoading)
        assertEquals(1, repository.loginCalls)
        response.complete(AppResult.Success(testUser))
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun passwordVisibilityToggleDoesNotChangePassword() {
        val vm = LoginViewModel(FakeAuthRepository())
        vm.onPasswordChange("Secreto123")
        assertFalse(vm.state.value.isPasswordVisible)
        vm.togglePasswordVisibility()
        assertTrue(vm.state.value.isPasswordVisible)
        assertEquals("Secreto123", vm.state.value.password)
        vm.togglePasswordVisibility()
        assertFalse(vm.state.value.isPasswordVisible)
    }

    @Test fun sameViewModelStorePreservesFormAndInjectedFactory() = runTest {
        val repository = FakeAuthRepository()
        val store = ViewModelStore()
        try {
            val factory = AuthViewModelFactories.login(repository)
            val first = ViewModelProvider(store, factory)[LoginViewModel::class.java]
            first.onCorreoChange("ana@example.com")
            first.onPasswordChange("SoloEnMemoria123")
            val recreatedOwner = ViewModelProvider(store, factory)[LoginViewModel::class.java]
            assertSame(first, recreatedOwner)
            assertEquals("ana@example.com", recreatedOwner.state.value.correo)
            assertEquals("SoloEnMemoria123", recreatedOwner.state.value.password)
            recreatedOwner.submit()
            advanceUntilIdle()
            assertEquals(1, repository.loginCalls)
        } finally { store.clear() }
    }

    @Test fun clearingOwnerCancelsRequestWithoutAuthenticationError() = runTest {
        var cancelled = false
        val repository = FakeAuthRepository().apply {
            login = {
                try { awaitCancellation() }
                catch (error: CancellationException) { cancelled = true; throw error }
            }
        }
        val store = ViewModelStore()
        val vm = ViewModelProvider(store, AuthViewModelFactories.login(repository))[LoginViewModel::class.java]
        vm.onCorreoChange("ana@example.com")
        vm.onPasswordChange("ClavePrueba123")
        vm.submit()
        runCurrent()
        assertTrue(vm.state.value.isLoading)
        store.clear()
        advanceUntilIdle()
        assertTrue(cancelled)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.authenticatedUser)
        assertNull(vm.state.value.errorMessage)
    }
}
