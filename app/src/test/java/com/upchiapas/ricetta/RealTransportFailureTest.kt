package com.upchiapas.ricetta

import com.upchiapas.ricetta.data.repository.AuthRepositoryImpl
import com.upchiapas.ricetta.data.repository.RecipeRepositoryImpl
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test

class RealTransportFailureTest {
    @Test fun disconnectedSocketIsConnectionFailure() = runTest {
        NetworkFixture().use { network ->
            network.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            val result = RecipeRepositoryImpl(network.recetasApi).obtenerRecetas()
            assertEquals(AppResult.Failure(DataError.NoInternet), result)
        }
    }

    @Test fun realReadTimeoutIsTyped() = runTest {
        NetworkFixture(callTimeoutMillis = 3000, readTimeoutMillis = 150).use { network ->
            network.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val result = RecipeRepositoryImpl(network.recetasApi).obtenerRecetas()
            assertEquals(AppResult.Failure(DataError.Timeout), result)
            assertEquals("/recetas", network.request().path)
        }
    }

    @Test fun realCallTimeoutIsTyped() = runTest {
        NetworkFixture(callTimeoutMillis = 150, readTimeoutMillis = 3000).use { network ->
            network.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val result = RecipeRepositoryImpl(network.recetasApi).obtenerRecetas()
            assertEquals(AppResult.Failure(DataError.Timeout), result)
            assertEquals("/recetas", network.request().path)
        }
    }

    @Test fun failedAuthenticationDoesNotCreateSession() = runTest {
        NetworkFixture().use { network ->
            val repository = AuthRepositoryImpl(network.authApi, network.storage)
            network.respond("""{"detail":"Credenciales incorrectas"}""", 401)
            assertEquals(
                AppResult.Failure(DataError.Unauthorized),
                repository.iniciarSesion("ana@example.com", "wrong-password")
            )
            assertNull(network.storage.getToken())
            assertFalse(network.storage.hasSession())

            network.respond("""{"detail":"Correo ya registrado"}""", 409)
            assertEquals(
                AppResult.Failure(DataError.Conflict),
                repository.registrar("Ana", "ana@example.com", "ClavePrueba123")
            )
            assertNull(network.storage.getToken())
            assertFalse(network.storage.hasSession())
        }
    }
}
