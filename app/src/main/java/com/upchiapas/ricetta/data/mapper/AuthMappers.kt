package com.upchiapas.ricetta.data.mapper

import com.upchiapas.ricetta.data.dto.AuthResponseDto
import com.upchiapas.ricetta.data.dto.UsuarioDto
import com.upchiapas.ricetta.domain.model.Usuario
import kotlinx.serialization.SerializationException

fun UsuarioDto.toDomain(): Usuario {
    if (id <= 0 || nombre.isBlank() || correo.isBlank()) {
        throw SerializationException("La respuesta contiene un usuario inválido.")
    }
    return Usuario(id = id, nombre = nombre, correo = correo)
}

fun AuthResponseDto.toDomain(): Usuario {
    if (accessToken.isBlank() || tokenType != "bearer") {
        throw SerializationException("La respuesta de autenticación es inválida.")
    }
    return usuario.toDomain()
}
