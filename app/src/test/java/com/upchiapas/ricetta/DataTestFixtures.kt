package com.upchiapas.ricetta

import com.upchiapas.ricetta.core.network.AuthInterceptor
import com.upchiapas.ricetta.data.remote.AuthApi
import com.upchiapas.ricetta.data.remote.RecetasApi
import com.upchiapas.ricetta.domain.session.SessionStorage
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

internal const val RECIPE_JSON =
    """{"id":1,"nombre":"Pasta","porciones":2,"tiempoPreparacionMin":25,"dificultad":"FACIL","esVegetariana":true,"ingredientes":"Tomate\nCebolla","preparacion":"Lavar\nCocinar"}"""
internal const val AUTH_JSON =
    """{"accessToken":"test-token","tokenType":"bearer","usuario":{"id":1,"nombre":"Ana","correo":"ana@example.com"}}"""

internal val testJson = Json { ignoreUnknownKeys = true }

internal class FakeSessionStorage(initialToken: String? = null) : SessionStorage {
    var storedToken: String? = initialToken
        private set

    override fun getToken(): String? = storedToken
    override suspend fun hasSession(): Boolean = storedToken != null
    override suspend fun saveToken(token: String) { storedToken = token }
    override suspend fun clearToken() { storedToken = null }
}

internal class NetworkFixture(
    val storage: FakeSessionStorage = FakeSessionStorage(),
    callTimeoutMillis: Long = 3000,
    readTimeoutMillis: Long = 3000
) : Closeable {
    val server = MockWebServer().apply { start() }
    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(storage))
        .callTimeout(callTimeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(client)
        .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
        .build()
    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val recetasApi: RecetasApi = retrofit.create(RecetasApi::class.java)

    fun respond(body: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code)
            .setHeader("Content-Type", "application/json").setBody(body))
    }

    fun request(): RecordedRequest =
        checkNotNull(server.takeRequest(2, TimeUnit.SECONDS)) { "No se recibió la petición" }

    override fun close() {
        server.shutdown()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }
}
