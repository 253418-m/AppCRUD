package com.upchiapas.ricetta.auth

import com.upchiapas.ricetta.domain.model.Usuario
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.session.SessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

internal val testUser = Usuario(7, "Ana", "ana@example.com")

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

internal class FakeAuthRepository : AuthRepository {
    var loginCalls = 0
    var registerCalls = 0
    var logoutCalls = 0
    var lastLogin: Pair<String, String>? = null
    var lastRegister: Triple<String, String, String>? = null
    var login: suspend () -> AppResult<Usuario> = { AppResult.Success(testUser) }
    var register: suspend () -> AppResult<Usuario> = { AppResult.Success(testUser) }
    var logout: suspend () -> AppResult<Unit> = { AppResult.Success(Unit) }

    override suspend fun iniciarSesion(correo: String, password: String): AppResult<Usuario> {
        loginCalls++
        lastLogin = correo to password
        return login()
    }
    override suspend fun registrar(nombre: String, correo: String, password: String): AppResult<Usuario> {
        registerCalls++
        lastRegister = Triple(nombre, correo, password)
        return register()
    }
    override suspend fun cerrarSesion(): AppResult<Unit> {
        logoutCalls++
        return logout()
    }
}

internal class FakeSessionStorage(var active: Boolean = false) : SessionStorage {
    var checkCalls = 0
    var check: (suspend () -> Boolean)? = null
    override fun getToken(): String? = if (active) "token-ficticio-de-pruebas" else null
    override suspend fun hasSession(): Boolean {
        checkCalls++
        return check?.invoke() ?: active
    }
    override suspend fun saveToken(token: String) { active = true }
    override suspend fun clearToken() { active = false }
}
