package com.upchiapas.ricetta.presentation.auth.login

import com.upchiapas.ricetta.domain.model.Usuario

data class LoginUiState(
    val correo: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val correoError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedUser: Usuario? = null,
)
