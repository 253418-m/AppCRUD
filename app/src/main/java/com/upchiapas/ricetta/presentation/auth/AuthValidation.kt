package com.upchiapas.ricetta.presentation.auth

object AuthValidation {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$")

    fun correoError(correo: String): String? {
        val value = correo.trim()
        return when {
            value.isEmpty() -> "Ingresa tu correo electrónico."
            value.length > 254 || !emailPattern.matches(value) ->
                "Ingresa un correo electrónico válido."
            else -> null
        }
    }

    fun nombreError(nombre: String): String? = when {
        nombre.isBlank() -> "Ingresa tu nombre completo."
        nombre.trim().length > 120 -> "El nombre no debe superar 120 caracteres."
        else -> null
    }

    fun passwordError(password: String, registration: Boolean = false): String? = when {
        password.isEmpty() -> "Ingresa tu contraseña."
        registration && password.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
        password.length > 128 -> "La contraseña no debe superar 128 caracteres."
        else -> null
    }

    fun confirmationError(password: String, confirmation: String): String? = when {
        confirmation.isEmpty() -> "Confirma tu contraseña."
        password != confirmation -> "Las contraseñas no coinciden."
        else -> null
    }
}
