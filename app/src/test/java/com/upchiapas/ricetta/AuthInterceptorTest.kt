package com.upchiapas.ricetta

import com.upchiapas.ricetta.data.dto.GuardarRecetaDto
import com.upchiapas.ricetta.data.dto.LoginRequestDto
import com.upchiapas.ricetta.data.dto.RegistroRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AuthInterceptorTest {
    @Test fun bearerIsAutomaticForAllProtectedRoutes() = runTest {
        NetworkFixture(FakeSessionStorage("test-token")).use { network ->
            network.respond("[]")
            network.recetasApi.obtenerRecetas()
            network.respond(RECIPE_JSON)
            network.recetasApi.obtenerReceta(1)
            network.respond(RECIPE_JSON, 201)
            network.recetasApi.crearReceta(GuardarRecetaDto("Pasta", 2, 25, "FACIL", true, "Tomate", "Cocinar"))
            network.respond(RECIPE_JSON)
            network.recetasApi.actualizarReceta(1, GuardarRecetaDto("Pasta", 2, 25, "FACIL", true, "Tomate", "Cocinar"))
            network.respond("", 204)
            network.recetasApi.eliminarReceta(1)
            repeat(5) {
                assertEquals("Bearer test-token", network.request().getHeader("Authorization"))
            }
        }
    }

    @Test fun absentSessionDoesNotCreateAuthorizationHeader() = runTest {
        NetworkFixture().use { network ->
            network.respond("[]")
            network.recetasApi.obtenerRecetas()
            assertNull(network.request().getHeader("Authorization"))
        }
    }

    @Test fun publicEndpointsDoNotReceiveStoredBearer() = runTest {
        NetworkFixture(FakeSessionStorage("test-token")).use { network ->
            network.respond(AUTH_JSON, 201)
            network.authApi.registrar(RegistroRequestDto("Ana", "ana@example.com", "ClavePrueba123"))
            network.respond(AUTH_JSON)
            network.authApi.iniciarSesion(LoginRequestDto("ana@example.com", "ClavePrueba123"))
            repeat(2) { assertNull(network.request().getHeader("Authorization")) }
        }
    }

    @Test fun tokenReplacementAndLogoutAffectNextRequest() = runTest {
        val storage = FakeSessionStorage("previous-test-token")
        NetworkFixture(storage).use { network ->
            storage.saveToken("replacement-test-token")
            network.respond("[]")
            network.recetasApi.obtenerRecetas()
            assertEquals("Bearer replacement-test-token", network.request().getHeader("Authorization"))
            storage.clearToken()
            network.respond("[]")
            network.recetasApi.obtenerRecetas()
            assertNull(network.request().getHeader("Authorization"))
        }
    }
}
