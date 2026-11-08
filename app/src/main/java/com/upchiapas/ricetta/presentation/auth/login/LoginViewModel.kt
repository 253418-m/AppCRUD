package com.upchiapas.ricetta.presentation.auth.login

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

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    private fun editable() = !_state.value.isLoading && _state.value.authenticatedUser == null

    fun onCorreoChange(value: String) {
        if (editable()) _state.update { it.copy(correo = value, correoError = null, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        if (editable()) _state.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        if (editable()) _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submit() {
        if (!editable()) return
        val input = _state.value
        val correoError = AuthValidation.correoError(input.correo)
        val passwordError = AuthValidation.passwordError(input.password)
        _state.value = input.copy(
            correoError = correoError, passwordError = passwordError, errorMessage = null
        )
        if (correoError != null || passwordError != null) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                when (val result = authRepository.iniciarSesion(
                    input.correo.trim().lowercase(Locale.ROOT), input.password
                )) {
                    is AppResult.Success -> _state.update {
                        it.copy(authenticatedUser = result.data, password = "", isPasswordVisible = false)
                    }
                    is AppResult.Failure -> _state.update {
                        it.copy(errorMessage = result.error.authMessage())
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
