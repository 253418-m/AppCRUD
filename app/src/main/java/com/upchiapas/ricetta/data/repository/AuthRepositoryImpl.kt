package com.upchiapas.ricetta.data.repository

import com.upchiapas.ricetta.core.network.apiResult
import com.upchiapas.ricetta.data.dto.LoginRequestDto
import com.upchiapas.ricetta.data.dto.RegistroRequestDto
import com.upchiapas.ricetta.data.mapper.toDomain
import com.upchiapas.ricetta.data.remote.AuthApi
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.session.SessionStorage

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionStorage: SessionStorage,
) : AuthRepository {
    override suspend fun registrar(nombre: String, correo: String, password: String) = apiResult {
        val response = authApi.registrar(RegistroRequestDto(nombre, correo, password))
        val user = response.toDomain()
        sessionStorage.saveToken(response.accessToken)
        user
    }

    override suspend fun iniciarSesion(correo: String, password: String) = apiResult {
        val response = authApi.iniciarSesion(LoginRequestDto(correo, password))
        val user = response.toDomain()
        sessionStorage.saveToken(response.accessToken)
        user
    }

    override suspend fun cerrarSesion() = apiResult {
        sessionStorage.clearToken()
    }
}
