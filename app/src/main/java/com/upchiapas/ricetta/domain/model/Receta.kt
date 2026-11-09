package com.upchiapas.ricetta.domain.model

enum class Dificultad {
    FACIL,
    MEDIA,
    DIFICIL
}

data class Receta(
    val id: Int,
    val nombre: String,
    val porciones: Int,
    val tiempoPreparacionMin: Int,
    val dificultad: Dificultad,
    val esVegetariana: Boolean,
    val ingredientes: String?,
    val preparacion: String?
)

data class GuardarReceta(
    val nombre: String,
    val porciones: Int,
    val tiempoPreparacionMin: Int,
    val dificultad: Dificultad,
    val esVegetariana: Boolean,
    val ingredientes: String,
    val preparacion: String
)
