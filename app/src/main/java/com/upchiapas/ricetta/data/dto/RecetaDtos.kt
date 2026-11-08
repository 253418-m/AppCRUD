package com.upchiapas.ricetta.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecetaDto(
    val id: Int,
    val nombre: String,
    val porciones: Int,
    val tiempoPreparacionMin: Int,
    val dificultad: String,
    val esVegetariana: Boolean
)

@Serializable
data class GuardarRecetaDto(
    val nombre: String,
    val porciones: Int,
    val tiempoPreparacionMin: Int,
    val dificultad: String,
    val esVegetariana: Boolean
)
