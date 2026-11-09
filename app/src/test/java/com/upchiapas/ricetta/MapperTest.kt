package com.upchiapas.ricetta

import com.upchiapas.ricetta.data.dto.AuthResponseDto
import com.upchiapas.ricetta.data.dto.RecetaDto
import com.upchiapas.ricetta.data.dto.UsuarioDto
import com.upchiapas.ricetta.data.mapper.toDomain
import com.upchiapas.ricetta.data.mapper.toDto
import com.upchiapas.ricetta.domain.model.Dificultad
import com.upchiapas.ricetta.domain.model.GuardarReceta
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test

class MapperTest {
    @Test fun recipeAndRequestKeepAllFields() {
        val dto = testJson.decodeFromString<RecetaDto>(RECIPE_JSON)
        val recipe = dto.toDomain()
        assertEquals(1, recipe.id)
        assertEquals("Pasta", recipe.nombre)
        assertEquals(2, recipe.porciones)
        assertEquals(25, recipe.tiempoPreparacionMin)
        assertEquals(Dificultad.FACIL, recipe.dificultad)
        assertTrue(recipe.esVegetariana)
        assertEquals("Tomate\nCebolla", recipe.ingredientes)
        assertEquals("Lavar\nCocinar", recipe.preparacion)
        val request = GuardarReceta(
            recipe.nombre, recipe.porciones, recipe.tiempoPreparacionMin,
            recipe.dificultad, recipe.esVegetariana,
            requireNotNull(recipe.ingredientes), requireNotNull(recipe.preparacion)
        ).toDto()
        assertEquals("Pasta", request.nombre)
        assertEquals(2, request.porciones)
        assertEquals(25, request.tiempoPreparacionMin)
        assertEquals("FACIL", request.dificultad)
        assertTrue(request.esVegetariana)
        assertEquals(recipe.ingredientes, request.ingredientes)
        assertEquals(recipe.preparacion, request.preparacion)
    }

    @Test fun historicalRecipeKeepsMissingTextsExplicit() {
        val body = RECIPE_JSON.replace("\"Tomate\\nCebolla\"", "null")
            .replace("\"Lavar\\nCocinar\"", "null")
        val recipe = testJson.decodeFromString<RecetaDto>(body).toDomain()
        assertNull(recipe.ingredientes)
        assertNull(recipe.preparacion)
    }

    @Test fun authMapsOnlyPublicUser() {
        val auth = testJson.decodeFromString<AuthResponseDto>(AUTH_JSON)
        assertEquals(UsuarioDto(1, "Ana", "ana@example.com").toDomain(), auth.toDomain())
        assertEquals("Ana", auth.toDomain().nombre)
    }

    @Test fun unknownJsonFieldsAreIgnored() {
        val body = RECIPE_JSON.dropLast(1) + ""","nuevoCampo":"compatible"}"""
        assertEquals(1, testJson.decodeFromString<RecetaDto>(body).id)
    }

    @Test fun requiredFieldsMissingOrNullAreRejected() {
        val invalidBodies = listOf(
            RECIPE_JSON.replace(""""nombre":"Pasta",""", ""),
            RECIPE_JSON.replace(""""nombre":"Pasta"""", """"nombre":null"""),
            RECIPE_JSON.replace(""""esVegetariana":true""", """"esVegetariana":null"""),
            RECIPE_JSON.replace(""""tiempoPreparacionMin":25""", """"tiempo_preparacion_min":25""")
        )
        invalidBodies.forEach { body ->
            assertThrows(SerializationException::class.java) {
                testJson.decodeFromString<RecetaDto>(body)
            }
        }
    }

    @Test fun invalidSemanticResponseIsNotReplacedByDefaults() {
        val original = testJson.decodeFromString<RecetaDto>(RECIPE_JSON)
        listOf(
            original.copy(id = 0),
            original.copy(nombre = " "),
            original.copy(porciones = 0),
            original.copy(tiempoPreparacionMin = 0),
            original.copy(dificultad = "DESCONOCIDA")
        ).forEach { invalid ->
            assertThrows(SerializationException::class.java) { invalid.toDomain() }
        }
    }

    @Test fun invalidAuthenticationResponseCannotBecomeSession() {
        val original = testJson.decodeFromString<AuthResponseDto>(AUTH_JSON)
        listOf(
            original.copy(accessToken = ""),
            original.copy(tokenType = "basic")
        ).forEach { invalid ->
            assertThrows(SerializationException::class.java) { invalid.toDomain() }
        }
    }
}
