package com.upchiapas.ricetta

import com.upchiapas.ricetta.data.dto.GuardarRecetaDto
import com.upchiapas.ricetta.data.dto.LoginRequestDto
import com.upchiapas.ricetta.data.dto.RegistroRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RetrofitContractTest {
    @Test fun getListAndDetailUseCorrectRoutes() = runTest {
        NetworkFixture().use { network ->
            network.respond("[$RECIPE_JSON]")
            assertEquals("Pasta", network.recetasApi.obtenerRecetas().single().nombre)
            val list = network.request()
            assertEquals("GET", list.method)
            assertEquals("/recetas", list.path)

            network.respond(RECIPE_JSON)
            assertEquals(1, network.recetasApi.obtenerReceta(1).id)
            val detail = network.request()
            assertEquals("GET", detail.method)
            assertEquals("/recetas/1", detail.path)
        }
    }

    @Test fun createAndUpdateSendOnlyEditableCamelCaseFields() = runTest {
        NetworkFixture().use { network ->
            val body = GuardarRecetaDto("Pasta", 2, 25, "FACIL", true,
                "Tomate\nCebolla", "Lavar\nCocinar")
            val expected = testJson.parseToJsonElement(
                """{"nombre":"Pasta","porciones":2,"tiempoPreparacionMin":25,"dificultad":"FACIL","esVegetariana":true,"ingredientes":"Tomate\nCebolla","preparacion":"Lavar\nCocinar"}"""
            )
            network.respond(RECIPE_JSON, 201)
            assertEquals(1, network.recetasApi.crearReceta(body).id)
            val create = network.request()
            assertEquals("POST", create.method)
            assertEquals("/recetas", create.path)
            assertEquals(expected, testJson.parseToJsonElement(create.body.readUtf8()))
            assertTrue(create.getHeader("Content-Type").orEmpty().startsWith("application/json"))

            network.respond(RECIPE_JSON)
            assertEquals(1, network.recetasApi.actualizarReceta(1, body).id)
            val update = network.request()
            assertEquals("PUT", update.method)
            assertEquals("/recetas/1", update.path)
            assertEquals(expected, testJson.parseToJsonElement(update.body.readUtf8()))
        }
    }

    @Test fun delete204ReturnsUnitWithoutArtificialJson() = runTest {
        NetworkFixture().use { network ->
            network.respond("", 204)
            val result: Unit = network.recetasApi.eliminarReceta(1)
            assertEquals(Unit, result)
            val request = network.request()
            assertEquals("DELETE", request.method)
            assertEquals("/recetas/1", request.path)
            assertEquals(0L, request.bodySize)
        }
    }

    @Test fun registerAndLoginUseExactJsonContract() = runTest {
        NetworkFixture().use { network ->
            network.respond(AUTH_JSON, 201)
            val registered = network.authApi.registrar(
                RegistroRequestDto("Ana", "ana@example.com", "ClavePrueba123")
            )
            assertEquals("test-token", registered.accessToken)
            val register = network.request()
            assertEquals("POST", register.method)
            assertEquals("/register", register.path)
            assertEquals(
                testJson.parseToJsonElement(
                    """{"nombre":"Ana","correo":"ana@example.com","password":"ClavePrueba123"}"""
                ),
                testJson.parseToJsonElement(register.body.readUtf8())
            )
            network.respond(AUTH_JSON)
            assertEquals("bearer", network.authApi.iniciarSesion(
                LoginRequestDto("ana@example.com", "ClavePrueba123")
            ).tokenType)
            val login = network.request()
            assertEquals("POST", login.method)
            assertEquals("/login", login.path)
            assertEquals(
                testJson.parseToJsonElement(
                    """{"correo":"ana@example.com","password":"ClavePrueba123"}"""
                ),
                testJson.parseToJsonElement(login.body.readUtf8())
            )
        }
    }
}
