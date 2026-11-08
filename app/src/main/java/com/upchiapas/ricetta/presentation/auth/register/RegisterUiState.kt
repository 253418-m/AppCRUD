package com.upchiapas.ricetta.presentation.auth.register

import com.upchiapas.ricetta.domain.model.Usuario

data class RegisterUiState(
    val nombre: String = "",
    val correo: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val nombreError: String? = null,
    val correoError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedUser: Usuario? = null,
)
