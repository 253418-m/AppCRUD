package com.upchiapas.ricetta

import com.upchiapas.ricetta.data.dto.AuthResponseDto
import com.upchiapas.ricetta.data.dto.GuardarRecetaDto
import com.upchiapas.ricetta.data.dto.LoginRequestDto
import com.upchiapas.ricetta.data.dto.RecetaDto
import com.upchiapas.ricetta.data.dto.RegistroRequestDto
import com.upchiapas.ricetta.data.remote.AuthApi
import com.upchiapas.ricetta.data.remote.RecetasApi
import com.upchiapas.ricetta.data.repository.AuthRepositoryImpl
import com.upchiapas.ricetta.data.repository.RecipeRepositoryImpl
import com.upchiapas.ricetta.domain.model.Dificultad
import com.upchiapas.ricetta.domain.model.GuardarReceta
import com.upchiapas.ricetta.domain.model.Receta
import com.upchiapas.ricetta.domain.model.Usuario
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.repository.RecipeRepository
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.domain.session.SessionStorage
import com.upchiapas.ricetta.domain.session.SessionStorageException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RepositoryBehaviorTest {
    @Test fun successfulAuthPersistsTokenAndExposesOnlyUser() = runTest {
        NetworkFixture().use { network ->
            val repository: AuthRepository = AuthRepositoryImpl(network.authApi, network.storage)
            network.respond(AUTH_JSON, 201)
            assertEquals(
                AppResult.Success(Usuario(1, "Ana", "ana@example.com")),
                repository.registrar("Ana", "ana@example.com", "ClavePrueba123")
            )
            assertEquals("test-token", network.storage.getToken())
            assertTrue(network.storage.hasSession())
            network.respond(AUTH_JSON.replace("test-token", "new-test-token"))
            assertTrue(repository.iniciarSesion("ana@example.com", "ClavePrueba123") is AppResult.Success)
            assertEquals("new-test-token", network.storage.getToken())
            assertEquals(AppResult.Success(Unit), repository.cerrarSesion())
            assertFalse(network.storage.hasSession())
            assertNull(network.storage.getToken())
        }
    }

    @Test fun failedLoginDoesNotReplaceExistingSession() = runTest {
        NetworkFixture(FakeSessionStorage("previous-test-token")).use { network ->
            val repository = AuthRepositoryImpl(network.authApi, network.storage)
            network.respond("""{"detail":"Credenciales incorrectas"}""", 401)
            assertEquals(
                AppResult.Failure(DataError.Unauthorized),
                repository.iniciarSesion("ana@example.com", "wrong-password")
            )
            assertEquals("previous-test-token", network.storage.getToken())
        }
    }

    @Test fun malformedAuthCannotPersistAnyToken() = runTest {
        NetworkFixture().use { network ->
            val repository = AuthRepositoryImpl(network.authApi, network.storage)
            network.respond(AUTH_JSON.replace("test-token", ""))
            assertEquals(
                AppResult.Failure(DataError.Serialization),
                repository.iniciarSesion("ana@example.com", "ClavePrueba123")
            )
            assertNull(network.storage.getToken())
        }
    }

    @Test fun repositoryMapsEveryRecipeOperationToDomain() = runTest {
        NetworkFixture(FakeSessionStorage("test-token")).use { network ->
            val repository: RecipeRepository = RecipeRepositoryImpl(network.recetasApi)
            val expected = Receta(1, "Pasta", 2, 25, Dificultad.FACIL, true)
            val body = GuardarReceta("Pasta", 2, 25, Dificultad.FACIL, true)
            network.respond("[$RECIPE_JSON]")
            assertEquals(AppResult.Success(listOf(expected)), repository.obtenerRecetas())
            network.respond(RECIPE_JSON)
            assertEquals(AppResult.Success(expected), repository.obtenerReceta(1))
            network.respond(RECIPE_JSON, 201)
            assertEquals(AppResult.Success(expected), repository.crearReceta(body))
            network.respond(RECIPE_JSON)
            assertEquals(AppResult.Success(expected), repository.actualizarReceta(1, body))
            network.respond("", 204)
            assertEquals(AppResult.Success(Unit), repository.eliminarReceta(1))
        }
    }

    @Test fun repositoryTranslatesRealHttpErrors() = runTest {
        NetworkFixture().use { network ->
            val repository = RecipeRepositoryImpl(network.recetasApi)
            val cases = mapOf(
                401 to DataError.Unauthorized, 404 to DataError.NotFound,
                409 to DataError.Conflict, 422 to DataError.Validation,
                500 to DataError.Server
            )
            for ((code, expected) in cases) {
                network.respond("""{"detail":"technical detail"}""", code)
                assertEquals(AppResult.Failure(expected), repository.obtenerRecetas())
            }
        }
    }

    @Test fun malformedJsonBecomesSerializationFailure() = runTest {
        NetworkFixture().use { network ->
            val repository = RecipeRepositoryImpl(network.recetasApi)
            network.respond("""[{"id":1}]""")
            assertEquals(AppResult.Failure(DataError.Serialization), repository.obtenerRecetas())
        }
    }

    @Test fun repositoryTranslatesConnectionFailure() = runTest {
        val api = object : RecetasApi by UnexpectedRecipeApi() {
            override suspend fun obtenerRecetas(): List<RecetaDto> = throw IOException("connection unavailable")
        }
        assertEquals(AppResult.Failure(DataError.NoInternet), RecipeRepositoryImpl(api).obtenerRecetas())
    }

    @Test fun recipeRepositoryPreservesCancellation() = runTest {
        val cancellation = CancellationException("cancelled recipe")
        val api = object : RecetasApi by UnexpectedRecipeApi() {
            override suspend fun obtenerRecetas(): List<RecetaDto> = throw cancellation
        }
        try {
            RecipeRepositoryImpl(api).obtenerRecetas()
            fail("CancellationException must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test fun authRepositoryPreservesCancellation() = runTest {
        val cancellation = CancellationException("cancelled authentication")
        val api = object : AuthApi {
            override suspend fun registrar(request: RegistroRequestDto): AuthResponseDto = throw cancellation
            override suspend fun iniciarSesion(request: LoginRequestDto): AuthResponseDto = throw cancellation
        }
        val storage = FakeSessionStorage()
        try {
            AuthRepositoryImpl(api, storage).iniciarSesion("ana@example.com", "ClavePrueba123")
            fail("CancellationException must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertNull(storage.getToken())
    }

    @Test fun storageFailureIsNotMisreportedAsNetworkFailure() = runTest {
        NetworkFixture().use { network ->
            val storage = object : SessionStorage by FakeSessionStorage() {
                override suspend fun saveToken(token: String) {
                    throw SessionStorageException("cannot persist")
                }
            }
            network.respond(AUTH_JSON)
            assertEquals(
                AppResult.Failure(DataError.Storage),
                AuthRepositoryImpl(network.authApi, storage)
                    .iniciarSesion("ana@example.com", "ClavePrueba123")
            )
        }
    }

    @Test fun publicRepositorySignaturesContainNoDtos() {
        listOf(AuthRepository::class.java, RecipeRepository::class.java).forEach { contract ->
            contract.declaredMethods.forEach { method ->
                assertFalse(method.toGenericString(), method.toGenericString().contains(".data.dto."))
                assertFalse(method.toGenericString(), method.toGenericString().contains("retrofit2."))
            }
        }
    }
}

private class UnexpectedRecipeApi : RecetasApi {
    override suspend fun obtenerRecetas(): List<RecetaDto> = error("Unexpected API invocation")
    override suspend fun obtenerReceta(id: Int): RecetaDto = error("Unexpected API invocation")
    override suspend fun crearReceta(request: GuardarRecetaDto): RecetaDto = error("Unexpected API invocation")
    override suspend fun actualizarReceta(id: Int, request: GuardarRecetaDto): RecetaDto = error("Unexpected API invocation")
    override suspend fun eliminarReceta(id: Int): Unit = error("Unexpected API invocation")
}
