package com.upchiapas.ricetta.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.session.SessionStorage
import com.upchiapas.ricetta.presentation.auth.AuthViewModelFactories
import com.upchiapas.ricetta.presentation.auth.components.LogoutConfirmation
import com.upchiapas.ricetta.presentation.auth.login.LoginScreen
import com.upchiapas.ricetta.presentation.auth.login.LoginViewModel
import com.upchiapas.ricetta.presentation.auth.register.RegisterScreen
import com.upchiapas.ricetta.presentation.auth.register.RegisterViewModel
import com.upchiapas.ricetta.presentation.session.SessionPhase
import com.upchiapas.ricetta.presentation.session.SessionViewModel

@Composable
fun RicettaApp(authRepository: AuthRepository, sessionStorage: SessionStorage) {
    val session: SessionViewModel = viewModel(
        factory = remember(authRepository, sessionStorage) {
            AuthViewModelFactories.session(authRepository, sessionStorage)
        }
    )
    val sessionState by session.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    if (sessionState.phase == SessionPhase.Checking) {
        StartupScreen(sessionState.isLoading, sessionState.errorMessage, session::retryCheckSession)
        return
    }

    LaunchedEffect(sessionState.phase) {
        val current = navController.currentDestination?.route
        val target = if (sessionState.phase == SessionPhase.SignedIn) "home_provisional" else "login"
        val alreadyInFlow = if (sessionState.phase == SessionPhase.SignedIn) {
            current == "home_provisional"
        } else current == "login" || current == "register"
        if (!alreadyInFlow) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController, startDestination = "startup") {
        composable("startup") { StartupScreen(true, null, session::retryCheckSession) }
        composable("login") {
            if (sessionState.phase == SessionPhase.SignedOut) {
                val login: LoginViewModel = viewModel(
                    factory = remember(authRepository) { AuthViewModelFactories.login(authRepository) }
                )
                LoginRoute(login, session::onAuthenticated) {
                    navController.navigate("register") { launchSingleTop = true }
                }
            }
        }
        composable("register") {
            if (sessionState.phase == SessionPhase.SignedOut) {
                val register: RegisterViewModel = viewModel(
                    factory = remember(authRepository) { AuthViewModelFactories.register(authRepository) }
                )
                RegisterRoute(register, session::onAuthenticated) {
                    navController.popBackStack("login", inclusive = false)
                }
            }
        }
        composable("home_provisional") {
            if (sessionState.phase == SessionPhase.SignedIn) {
                ProvisionalHome(session::requestLogout,
                    if (sessionState.showLogoutDialog) null else sessionState.errorMessage)
                if (sessionState.showLogoutDialog) {
                    LogoutConfirmation(
                        isLoading = sessionState.isLoggingOut,
                        errorMessage = sessionState.errorMessage,
                        onCancel = session::cancelLogout,
                        onConfirm = session::confirmLogout,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginRoute(model: LoginViewModel, onAuthenticated: () -> Unit, onRegister: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.authenticatedUser) {
        if (state.authenticatedUser != null) onAuthenticated()
    }
    LoginScreen(state, model::onCorreoChange, model::onPasswordChange,
        model::togglePasswordVisibility, model::submit, onRegister)
}

@Composable
private fun RegisterRoute(model: RegisterViewModel, onAuthenticated: () -> Unit, onLogin: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.authenticatedUser) {
        if (state.authenticatedUser != null) onAuthenticated()
    }
    RegisterScreen(state, model::onNombreChange, model::onCorreoChange, model::onPasswordChange,
        model::onConfirmPasswordChange, model::togglePasswordVisibility,
        model::toggleConfirmPasswordVisibility, model::submit, onLogin)
}

@Composable
private fun StartupScreen(loading: Boolean, error: String?, onRetry: () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp).testTag("startup_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            if (loading) CircularProgressIndicator(Modifier.size(28.dp))
            if (error != null) {
                Text(error)
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun ProvisionalHome(onLogout: () -> Unit, error: String?) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp).testTag("home_provisional"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("Ricetta", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text("Sesión iniciada")
            Spacer(Modifier.height(24.dp))
            Button(onClick = onLogout, modifier = Modifier.testTag("home_logout")) {
                Text("Cerrar sesión")
            }
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
