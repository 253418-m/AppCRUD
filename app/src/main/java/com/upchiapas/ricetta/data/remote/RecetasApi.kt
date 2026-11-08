package com.upchiapas.ricetta.data.remote

import com.upchiapas.ricetta.data.dto.GuardarRecetaDto
import com.upchiapas.ricetta.data.dto.RecetaDto
import retrofit2.http.*

interface RecetasApi {
    @GET("recetas")
    suspend fun obtenerRecetas(): List<RecetaDto>

    @GET("recetas/{id}")
    suspend fun obtenerReceta(@Path("id") id: Int): RecetaDto

    @POST("recetas")
    suspend fun crearReceta(@Body request: GuardarRecetaDto): RecetaDto

    @PUT("recetas/{id}")
    suspend fun actualizarReceta(@Path("id") id: Int, @Body request: GuardarRecetaDto): RecetaDto

    @DELETE("recetas/{id}")
    suspend fun eliminarReceta(@Path("id") id: Int): Unit
}
