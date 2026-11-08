package com.upchiapas.ricetta.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.domain.session.SessionStorage
import com.upchiapas.ricetta.presentation.auth.authMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val authRepository: AuthRepository,
    private val sessionStorage: SessionStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(SessionUiState())
    val state = _state.asStateFlow()
    private var checkJob: Job? = null
    private var generation = 0

    init { retryCheckSession() }

    fun retryCheckSession() {
        if (_state.value.phase != SessionPhase.Checking || checkJob?.isActive == true) return
        val version = ++generation
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        checkJob = viewModelScope.launch {
            try {
                val exists = sessionStorage.hasSession()
                if (version == generation) {
                    _state.value = SessionUiState(
                        phase = if (exists) SessionPhase.SignedIn else SessionPhase.SignedOut,
                        isLoading = false
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (version == generation) _state.update {
                    it.copy(isLoading = false, errorMessage = "No pudimos recuperar la sesión. Inténtalo nuevamente.")
                }
            }
        }
    }

    fun onAuthenticated() {
        generation++
        checkJob?.cancel()
        _state.value = SessionUiState(phase = SessionPhase.SignedIn, isLoading = false)
    }

    fun requestLogout() {
        if (_state.value.phase == SessionPhase.SignedIn && !_state.value.isLoggingOut) {
            _state.update { it.copy(showLogoutDialog = true, errorMessage = null) }
        }
    }

    fun cancelLogout() {
        if (!_state.value.isLoggingOut) {
            _state.update { it.copy(showLogoutDialog = false, errorMessage = null) }
        }
    }

    fun confirmLogout() {
        if (_state.value.showLogoutDialog) performLogout()
    }

    fun onProtectedError(error: DataError) {
        if (_state.value.phase != SessionPhase.SignedIn) return
        if (error == DataError.Unauthorized) performLogout()
        else _state.update { it.copy(errorMessage = error.authMessage()) }
    }

    private fun performLogout() {
        if (_state.value.phase != SessionPhase.SignedIn || _state.value.isLoggingOut) return
        val version = ++generation
        _state.update { it.copy(isLoggingOut = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val result = authRepository.cerrarSesion()) {
                    is AppResult.Success -> if (version == generation) {
                        _state.value = SessionUiState(phase = SessionPhase.SignedOut, isLoading = false)
                    }
                    is AppResult.Failure -> if (version == generation) {
                        _state.update { it.copy(errorMessage = result.error.authMessage()) }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (version == generation) _state.update {
                    it.copy(errorMessage = DataError.Unknown.authMessage())
                }
            } finally {
                if (version == generation) _state.update { it.copy(isLoggingOut = false) }
            }
        }
    }
}
