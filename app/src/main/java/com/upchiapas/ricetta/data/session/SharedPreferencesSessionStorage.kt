package com.upchiapas.ricetta.data.session

import android.content.SharedPreferences
import com.upchiapas.ricetta.domain.session.SessionStorage
import com.upchiapas.ricetta.domain.session.SessionStorageException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPreferencesSessionStorage(
    private val preferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SessionStorage {
    // OkHttp llama este método en su hilo de trabajo; la restauración usa hasSession.
    override fun getToken(): String? = preferences.getString(TOKEN_KEY, null)

    override suspend fun hasSession(): Boolean = withContext(ioDispatcher) {
        !getToken().isNullOrBlank()
    }

    override suspend fun saveToken(token: String) = withContext(ioDispatcher) {
        require(token.isNotBlank()) { "El token no puede estar vacío" }
        if (!preferences.edit().putString(TOKEN_KEY, token).commit()) {
            throw SessionStorageException("No se pudo guardar la sesión")
        }
    }

    override suspend fun clearToken() = withContext(ioDispatcher) {
        if (!preferences.edit().remove(TOKEN_KEY).commit()) {
            throw SessionStorageException("No se pudo eliminar la sesión")
        }
    }

    companion object {
        const val PREFERENCES_NAME = "ricetta_session"
        private const val TOKEN_KEY = "access_token"
    }
}
