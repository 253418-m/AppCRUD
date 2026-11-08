package com.upchiapas.ricetta.data.repository

import com.upchiapas.ricetta.core.network.apiResult
import com.upchiapas.ricetta.data.mapper.toDomain
import com.upchiapas.ricetta.data.mapper.toDto
import com.upchiapas.ricetta.data.remote.RecetasApi
import com.upchiapas.ricetta.domain.model.GuardarReceta
import com.upchiapas.ricetta.domain.repository.RecipeRepository

class RecipeRepositoryImpl(private val recetasApi: RecetasApi) : RecipeRepository {
    override suspend fun obtenerRecetas() = apiResult {
        recetasApi.obtenerRecetas().map { it.toDomain() }
    }

    override suspend fun obtenerReceta(id: Int) = apiResult {
        recetasApi.obtenerReceta(id).toDomain()
    }

    override suspend fun crearReceta(receta: GuardarReceta) = apiResult {
        recetasApi.crearReceta(receta.toDto()).toDomain()
    }

    override suspend fun actualizarReceta(id: Int, receta: GuardarReceta) = apiResult {
        recetasApi.actualizarReceta(id, receta.toDto()).toDomain()
    }

    override suspend fun eliminarReceta(id: Int) = apiResult {
        recetasApi.eliminarReceta(id)
    }
}
