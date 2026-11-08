package com.upchiapas.ricetta.domain.session

interface SessionStorage {
    /**
     * Lectura síncrona para el interceptor en su hilo de trabajo.
     * Para consultar la sesión desde una corrutina, usar [hasSession].
     */
    fun getToken(): String?

    suspend fun hasSession(): Boolean
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}

class SessionStorageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
