package com.upchiapas.ricetta.domain.repository

import com.upchiapas.ricetta.domain.model.Usuario
import com.upchiapas.ricetta.domain.result.AppResult

interface AuthRepository {
    suspend fun registrar(nombre: String, correo: String, password: String): AppResult<Usuario>
    suspend fun iniciarSesion(correo: String, password: String): AppResult<Usuario>
    suspend fun cerrarSesion(): AppResult<Unit>
}
