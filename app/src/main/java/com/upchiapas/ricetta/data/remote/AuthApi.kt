package com.upchiapas.ricetta.data.remote

import com.upchiapas.ricetta.data.dto.AuthResponseDto
import com.upchiapas.ricetta.data.dto.LoginRequestDto
import com.upchiapas.ricetta.data.dto.RegistroRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("register")
    suspend fun registrar(@Body request: RegistroRequestDto): AuthResponseDto

    @POST("login")
    suspend fun iniciarSesion(@Body request: LoginRequestDto): AuthResponseDto
}
