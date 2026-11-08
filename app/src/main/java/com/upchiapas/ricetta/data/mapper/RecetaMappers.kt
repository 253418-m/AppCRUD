package com.upchiapas.ricetta.data.mapper

import com.upchiapas.ricetta.data.dto.GuardarRecetaDto
import com.upchiapas.ricetta.data.dto.RecetaDto
import com.upchiapas.ricetta.domain.model.Dificultad
import com.upchiapas.ricetta.domain.model.GuardarReceta
import com.upchiapas.ricetta.domain.model.Receta
import kotlinx.serialization.SerializationException

fun RecetaDto.toDomain(): Receta {
    if (id <= 0 || nombre.isBlank() || porciones < 1 || tiempoPreparacionMin < 1) {
        throw SerializationException("La respuesta contiene una receta inválida.")
    }
    val dificultadDominio = Dificultad.entries.firstOrNull { it.name == dificultad }
        ?: throw SerializationException("La respuesta contiene una dificultad inválida.")
    return Receta(
        id = id,
        nombre = nombre,
        porciones = porciones,
        tiempoPreparacionMin = tiempoPreparacionMin,
        dificultad = dificultadDominio,
        esVegetariana = esVegetariana
    )
}

fun GuardarReceta.toDto(): GuardarRecetaDto = GuardarRecetaDto(
    nombre = nombre,
    porciones = porciones,
    tiempoPreparacionMin = tiempoPreparacionMin,
    dificultad = dificultad.name,
    esVegetariana = esVegetariana
)
