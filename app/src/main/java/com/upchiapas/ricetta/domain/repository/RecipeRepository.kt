package com.upchiapas.ricetta.domain.repository

import com.upchiapas.ricetta.domain.model.GuardarReceta
import com.upchiapas.ricetta.domain.model.Receta
import com.upchiapas.ricetta.domain.result.AppResult

interface RecipeRepository {
    suspend fun obtenerRecetas(): AppResult<List<Receta>>
    suspend fun obtenerReceta(id: Int): AppResult<Receta>
    suspend fun crearReceta(receta: GuardarReceta): AppResult<Receta>
    suspend fun actualizarReceta(id: Int, receta: GuardarReceta): AppResult<Receta>
    suspend fun eliminarReceta(id: Int): AppResult<Unit>
}
