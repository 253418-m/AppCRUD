package com.upchiapas.ricetta.presentation.session

enum class SessionPhase { Checking, SignedOut, SignedIn }

data class SessionUiState(
    val phase: SessionPhase = SessionPhase.Checking,
    val isLoading: Boolean = true,
    val showLogoutDialog: Boolean = false,
    val isLoggingOut: Boolean = false,
    val errorMessage: String? = null,
)
