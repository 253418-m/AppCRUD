package com.upchiapas.ricetta.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.upchiapas.ricetta.core.network.AuthInterceptor
import com.upchiapas.ricetta.data.remote.AuthApi
import com.upchiapas.ricetta.data.remote.RecetasApi
import com.upchiapas.ricetta.data.repository.AuthRepositoryImpl
import com.upchiapas.ricetta.data.repository.RecipeRepositoryImpl
import com.upchiapas.ricetta.data.session.SharedPreferencesSessionStorage
import com.upchiapas.ricetta.domain.model.Dificultad
import com.upchiapas.ricetta.domain.model.GuardarReceta
import com.upchiapas.ricetta.domain.result.AppResult
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Ejecutar solo con fastApiBaseUrl apuntando a FastAPI real y una base aislada.
 * No se activa por defecto ni utiliza respuestas simuladas.
 */
@RunWith(AndroidJUnit4::class)
class FastApiIntegrationTest {
    @Test
    fun repositoriesAuthenticateAndCompleteCrudAgainstRealFastApi() = runBlocking {
        val configuredUrl = InstrumentationRegistry.getArguments().getString("fastApiBaseUrl")
        assumeTrue("Requiere argumento fastApiBaseUrl y backend de pruebas real", !configuredUrl.isNullOrBlank())
        val baseUrl = requireNotNull(configuredUrl)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferencesName = "fastapi-session-test-${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val storage = SharedPreferencesSessionStorage(preferences)
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(storage))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }
                    .asConverterFactory("application/json".toMediaType())
            )
            .build()
        val auth = AuthRepositoryImpl(retrofit.create(AuthApi::class.java), storage)
        val recipes = RecipeRepositoryImpl(retrofit.create(RecetasApi::class.java))
        var createdId: Int? = null

        try {
            val email = "android-${UUID.randomUUID()}@example.com"
            val password = "SoloPruebas123!"
            val registered = auth.registrar("Prueba Android", email, password).value()
            assertEquals(email, registered.correo)
            assertTrue(storage.hasSession())

            val reopenedStorage = SharedPreferencesSessionStorage(
                context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            )
            assertTrue(reopenedStorage.hasSession())
            // Evitar que una aserción fallida imprima los tokens reales.
            assertTrue(storage.getToken() == reopenedStorage.getToken())

            auth.cerrarSesion().value()
            assertFalse(storage.hasSession())
            val loggedIn = auth.iniciarSesion(email, password).value()
            assertEquals(registered, loggedIn)
            assertTrue(storage.hasSession())
            assertTrue(recipes.obtenerRecetas().value().isEmpty())

            val request = GuardarReceta(
                nombre = "Sopa de integración Android",
                porciones = 2,
                tiempoPreparacionMin = 20,
                dificultad = Dificultad.FACIL,
                esVegetariana = true,
                ingredientes = "Agua\nSal",
                preparacion = "Hervir\nServir"
            )
            val created = recipes.crearReceta(request).value()
            createdId = created.id
            assertEquals(request.nombre, created.nombre)
            assertEquals(request.porciones, created.porciones)
            assertEquals(request.tiempoPreparacionMin, created.tiempoPreparacionMin)
            assertEquals(request.dificultad, created.dificultad)
            assertTrue(created.esVegetariana)
            assertEquals(request.ingredientes, created.ingredientes)
            assertEquals(request.preparacion, created.preparacion)
            assertEquals(created, recipes.obtenerReceta(created.id).value())
            assertEquals(listOf(created), recipes.obtenerRecetas().value())

            val update = request.copy(
                nombre = "Sopa de integración actualizada",
                porciones = 4,
                dificultad = Dificultad.MEDIA,
                esVegetariana = false,
                ingredientes = "Agua\nSal\nTomate",
                preparacion = "Hervir\nAñadir tomate\nServir"
            )
            val updated = recipes.actualizarReceta(created.id, update).value()
            assertEquals(created.id, updated.id)
            assertEquals(update.nombre, updated.nombre)
            assertEquals(update.porciones, updated.porciones)
            assertEquals(update.dificultad, updated.dificultad)
            assertFalse(updated.esVegetariana)
            assertEquals(update.ingredientes, updated.ingredientes)
            assertEquals(update.preparacion, updated.preparacion)
            assertEquals(updated, recipes.obtenerReceta(created.id).value())
            assertEquals(listOf(updated), recipes.obtenerRecetas().value())

            recipes.eliminarReceta(created.id).value()
            createdId = null
            assertTrue(recipes.obtenerRecetas().value().isEmpty())
            auth.cerrarSesion().value()
            assertFalse(storage.hasSession())
        } finally {
            createdId?.let { recipes.eliminarReceta(it) }
            storage.clearToken()
            context.deleteSharedPreferences(preferencesName)
            httpClient.connectionPool.evictAll()
            httpClient.dispatcher.executorService.shutdown()
        }
    }

    private fun <T> AppResult<T>.value(): T = when (this) {
        is AppResult.Success -> data
        is AppResult.Failure -> throw AssertionError("Operación de repositorio falló: $error")
    }
}
