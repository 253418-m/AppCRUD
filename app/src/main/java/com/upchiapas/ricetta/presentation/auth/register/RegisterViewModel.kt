package com.upchiapas.ricetta.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.presentation.auth.AuthValidation
import com.upchiapas.ricetta.presentation.auth.authMessage
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state = _state.asStateFlow()

    private fun editable() = !_state.value.isLoading && _state.value.authenticatedUser == null

    fun onNombreChange(value: String) {
        if (editable()) _state.update { it.copy(nombre = value, nombreError = null, errorMessage = null) }
    }

    fun onCorreoChange(value: String) {
        if (editable()) _state.update { it.copy(correo = value, correoError = null, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        if (editable()) _state.update {
            it.copy(password = value, passwordError = null, confirmPasswordError = null, errorMessage = null)
        }
    }

    fun onConfirmPasswordChange(value: String) {
        if (editable()) _state.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null)
        }
    }

    fun togglePasswordVisibility() {
        if (editable()) _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        if (editable()) _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun submit() {
        if (!editable()) return
        val input = _state.value
        val nombreError = AuthValidation.nombreError(input.nombre)
        val correoError = AuthValidation.correoError(input.correo)
        val passwordError = AuthValidation.passwordError(input.password, registration = true)
        val confirmationError = AuthValidation.confirmationError(input.password, input.confirmPassword)
        _state.value = input.copy(
            nombreError = nombreError, correoError = correoError, passwordError = passwordError,
            confirmPasswordError = confirmationError, errorMessage = null
        )
        if (listOf(nombreError, correoError, passwordError, confirmationError).any { it != null }) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                when (val result = authRepository.registrar(
                    input.nombre.trim(), input.correo.trim().lowercase(Locale.ROOT), input.password
                )) {
                    is AppResult.Success -> _state.update {
                        it.copy(
                            authenticatedUser = result.data, password = "", confirmPassword = "",
                            isPasswordVisible = false, isConfirmPasswordVisible = false
                        )
                    }
                    is AppResult.Failure -> _state.update {
                        it.copy(
                            errorMessage = if (result.error == DataError.Conflict) null else result.error.authMessage(),
                            correoError = if (result.error == DataError.Conflict) result.error.authMessage() else null
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(errorMessage = DataError.Unknown.authMessage()) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
