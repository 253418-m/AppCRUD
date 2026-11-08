package com.upchiapas.ricetta.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.upchiapas.ricetta.domain.model.Usuario
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.presentation.auth.AuthViewModelFactories
import com.upchiapas.ricetta.presentation.auth.register.RegisterViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun fill(vm: RegisterViewModel, password: String = "ClavePrueba123", confirm: String = password) {
        vm.onNombreChange("Ana")
        vm.onCorreoChange("ana@example.com")
        vm.onPasswordChange(password)
        vm.onConfirmPasswordChange(confirm)
    }

    @Test fun requiredFieldsAreReportedWithoutSendingRequest() = runTest {
        val repository = FakeAuthRepository()
        val vm = RegisterViewModel(repository)
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.nombreError)
        assertNotNull(vm.state.value.correoError)
        assertNotNull(vm.state.value.passwordError)
        assertNotNull(vm.state.value.confirmPasswordError)
        assertEquals(0, repository.registerCalls)
    }

    @Test fun whitespaceNameIsRejected() = runTest {
        val repository = FakeAuthRepository()
        val vm = RegisterViewModel(repository)
        fill(vm)
        vm.onNombreChange("   ")
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.nombreError)
        assertEquals(0, repository.registerCalls)
    }

    @Test fun malformedEmailIsRejected() = runTest {
        val repository = FakeAuthRepository()
        val vm = RegisterViewModel(repository)
        fill(vm)
        vm.onCorreoChange("ana.example.com")
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.correoError)
        assertEquals(0, repository.registerCalls)
    }

    @Test fun shortPasswordIsRejected() = runTest {
        val repository = FakeAuthRepository()
        val vm = RegisterViewModel(repository)
        fill(vm, password = "1234567")
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.passwordError)
        assertEquals(0, repository.registerCalls)
    }

    @Test fun missingOrDifferentConfirmationIsRejected() = runTest {
        for (confirmation in listOf("", "Diferente123")) {
            val repository = FakeAuthRepository()
            val vm = RegisterViewModel(repository)
            fill(vm, confirm = confirmation)
            vm.submit()
            advanceUntilIdle()
            assertNotNull(vm.state.value.confirmPasswordError)
            assertEquals(0, repository.registerCalls)
        }
    }

    @Test fun successfulRegistrationDoesNotSendConfirmationAndClearsPasswords() = runTest {
        val repository = FakeAuthRepository()
        val vm = RegisterViewModel(repository)
        fill(vm, password = " ClavePrueba123 ")
        vm.onNombreChange(" Ana ")
        vm.onCorreoChange(" ana@example.com ")
        vm.submit()
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertEquals(Triple("Ana", "ana@example.com", " ClavePrueba123 "), repository.lastRegister)
        assertEquals(testUser, vm.state.value.authenticatedUser)
        assertEquals("", vm.state.value.password)
        assertEquals("", vm.state.value.confirmPassword)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun duplicateEmailPreservesFormAndAllowsRetry() = runTest {
        val repository = FakeAuthRepository().apply {
            register = { AppResult.Failure(DataError.Conflict) }
        }
        val vm = RegisterViewModel(repository)
        fill(vm)
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.errorMessage ?: vm.state.value.correoError)
        assertEquals("Ana", vm.state.value.nombre)
        assertEquals("ana@example.com", vm.state.value.correo)
        assertEquals("ClavePrueba123", vm.state.value.password)
        assertEquals("ClavePrueba123", vm.state.value.confirmPassword)
        assertFalse(vm.state.value.isLoading)
        repository.register = { AppResult.Success(testUser) }
        vm.onCorreoChange("otra@example.com")
        vm.submit()
        advanceUntilIdle()
        assertEquals(2, repository.registerCalls)
        assertEquals(testUser, vm.state.value.authenticatedUser)
    }

    @Test fun serverErrorIsRecoverableAndDoesNotAuthenticate() = runTest {
        val repository = FakeAuthRepository().apply { register = { AppResult.Failure(DataError.Server) } }
        val vm = RegisterViewModel(repository)
        fill(vm)
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.errorMessage)
        assertNull(vm.state.value.authenticatedUser)
        assertFalse(vm.state.value.isLoading)
        assertEquals("ClavePrueba123", vm.state.value.confirmPassword)
    }

    @Test fun loadingPreventsDuplicateRegistration() = runTest {
        val response = CompletableDeferred<AppResult<Usuario>>()
        val repository = FakeAuthRepository().apply { register = { response.await() } }
        val vm = RegisterViewModel(repository)
        fill(vm)
        vm.submit()
        vm.submit()
        runCurrent()
        vm.submit()
        assertTrue(vm.state.value.isLoading)
        assertEquals(1, repository.registerCalls)
        response.complete(AppResult.Success(testUser))
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun passwordVisibilityControlsAreIndependent() {
        val vm = RegisterViewModel(FakeAuthRepository())
        assertFalse(vm.state.value.isPasswordVisible)
        assertFalse(vm.state.value.isConfirmPasswordVisible)
        vm.togglePasswordVisibility()
        assertTrue(vm.state.value.isPasswordVisible)
        assertFalse(vm.state.value.isConfirmPasswordVisible)
        vm.toggleConfirmPasswordVisibility()
        assertTrue(vm.state.value.isPasswordVisible)
        assertTrue(vm.state.value.isConfirmPasswordVisible)
    }

    @Test fun factoryCreatesRegistrationViewModelWithProvidedRepository() = runTest {
        val repository = FakeAuthRepository()
        val store = ViewModelStore()
        try {
            val vm = ViewModelProvider(store, AuthViewModelFactories.register(repository))[RegisterViewModel::class.java]
            fill(vm)
            vm.submit()
            advanceUntilIdle()
            assertEquals(1, repository.registerCalls)
            assertEquals(testUser, vm.state.value.authenticatedUser)
        } finally { store.clear() }
    }
}
