package com.upchiapas.ricetta.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegistroRequestDto(
    val nombre: String,
    val correo: String,
    val password: String
)

@Serializable
data class LoginRequestDto(
    val correo: String,
    val password: String
)

@Serializable
data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val correo: String
)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val tokenType: String,
    val usuario: UsuarioDto
)
