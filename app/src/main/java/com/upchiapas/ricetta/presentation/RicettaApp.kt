package com.upchiapas.ricetta.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.repository.RecipeRepository
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.domain.session.SessionStorage
import com.upchiapas.ricetta.presentation.auth.AuthViewModelFactories
import com.upchiapas.ricetta.presentation.auth.components.LogoutConfirmation
import com.upchiapas.ricetta.presentation.auth.login.LoginScreen
import com.upchiapas.ricetta.presentation.auth.login.LoginViewModel
import com.upchiapas.ricetta.presentation.auth.register.RegisterScreen
import com.upchiapas.ricetta.presentation.auth.register.RegisterViewModel
import com.upchiapas.ricetta.presentation.recipe.*
import com.upchiapas.ricetta.presentation.session.SessionPhase
import com.upchiapas.ricetta.presentation.session.SessionViewModel

@Composable
fun RicettaApp(
    authRepository: AuthRepository,
    sessionStorage: SessionStorage,
    recipeRepository: RecipeRepository,
) {
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
        val target = if (sessionState.phase == SessionPhase.SignedIn) "home" else "login"
        val alreadyInFlow = if (sessionState.phase == SessionPhase.SignedIn) {
            current == "home" || current?.startsWith("recipe_") == true
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
        composable("home") { entry ->
            if (sessionState.phase == SessionPhase.SignedIn) {
                val model: RecipeListViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = remember(recipeRepository) { RecipeViewModelFactories.list(recipeRepository) }
                )
                val state by model.state.collectAsStateWithLifecycle()
                val feedback by entry.savedStateHandle.getStateFlow("feedback", "")
                    .collectAsStateWithLifecycle()
                DisposableEffect(entry.lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) model.refresh()
                    }
                    entry.lifecycle.addObserver(observer)
                    onDispose { entry.lifecycle.removeObserver(observer) }
                }
                LaunchedEffect(state.error) {
                    if (state.error == DataError.Unauthorized)
                        session.onProtectedError(DataError.Unauthorized)
                }
                LaunchedEffect(feedback) {
                    if (feedback.isNotEmpty()) {
                        kotlinx.coroutines.delay(3500)
                        entry.savedStateHandle["feedback"] = ""
                    }
                }
                RecipeHomeScreen(state,
                    onOpen = { navController.navigate("recipe_detail/$it") },
                    onCreate = { navController.navigate("recipe_create") },
                    onLogout = session::requestLogout,
                    onRetry = model::refresh,
                    feedback = feedback.ifEmpty { null },
                )
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
        composable(
            "recipe_detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entry ->
            if (sessionState.phase == SessionPhase.SignedIn) {
                val id = requireNotNull(entry.arguments).getInt("id")
                val model: RecipeDetailViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = remember(id, recipeRepository) {
                        RecipeViewModelFactories.detail(id, recipeRepository)
                    }
                )
                val state by model.state.collectAsStateWithLifecycle()
                val feedback by entry.savedStateHandle.getStateFlow("feedback", "")
                    .collectAsStateWithLifecycle()
                DisposableEffect(entry.lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) model.refresh()
                    }
                    entry.lifecycle.addObserver(observer)
                    onDispose { entry.lifecycle.removeObserver(observer) }
                }
                LaunchedEffect(feedback) {
                    if (feedback.isNotEmpty()) {
                        kotlinx.coroutines.delay(3500)
                        entry.savedStateHandle["feedback"] = ""
                    }
                }
                LaunchedEffect(state.error, state.deleteError) {
                    if (state.error == DataError.Unauthorized ||
                        state.deleteError == DataError.Unauthorized)
                        session.onProtectedError(DataError.Unauthorized)
                }
                LaunchedEffect(state.deleted) {
                    if (state.deleted) {
                        navController.getBackStackEntry("home").savedStateHandle["feedback"] =
                            "Receta eliminada correctamente"
                        navController.popBackStack("home", inclusive = false)
                    }
                }
                RecipeDetailScreen(state,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("recipe_edit/$id") },
                    onDelete = model::requestDelete,
                    onRetry = model::refresh,
                    feedback = feedback.ifEmpty { null },
                )
                if (state.showDeleteDialog)
                    RecipeDeleteDialog(state, model::cancelDelete, model::confirmDelete)
            }
        }
        composable("recipe_create") { entry ->
            if (sessionState.phase == SessionPhase.SignedIn) {
                RecipeFormRoute(
                    id = null, repository = recipeRepository,
                    onUnauthorized = { session.onProtectedError(DataError.Unauthorized) },
                    onCancel = { navController.popBackStack() },
                    onSaved = { _ ->
                        navController.getBackStackEntry("home").savedStateHandle["feedback"] =
                            "Receta creada correctamente"
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }
        }
        composable(
            "recipe_edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entry ->
            if (sessionState.phase == SessionPhase.SignedIn) {
                val id = requireNotNull(entry.arguments).getInt("id")
                RecipeFormRoute(
                    id = id, repository = recipeRepository,
                    onUnauthorized = { session.onProtectedError(DataError.Unauthorized) },
                    onCancel = { navController.popBackStack() },
                    onSaved = {
                        navController.getBackStackEntry("recipe_detail/{id}")
                            .savedStateHandle["feedback"] = "Receta actualizada correctamente"
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun RecipeFormRoute(
    id: Int?, repository: RecipeRepository,
    onUnauthorized: () -> Unit, onCancel: () -> Unit,
    onSaved: (com.upchiapas.ricetta.domain.model.Receta) -> Unit,
) {
    val model: RecipeFormViewModel = viewModel(
        key = "recipe_form_${id ?: "create"}",
        factory = remember(id, repository) { RecipeViewModelFactories.form(id, repository) }
    )
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.error) {
        if (state.error == DataError.Unauthorized) onUnauthorized()
    }
    LaunchedEffect(state.saved) { state.saved?.let(onSaved) }
    RecipeFormScreen(state, editing = id != null,
        onNombre = model::onNombre, onPorciones = model::onPorciones,
        onTiempo = model::onTiempo, onDificultad = model::onDificultad,
        onVegetariana = model::onVegetariana, onIngredientes = model::onIngredientes,
        onPreparacion = model::onPreparacion, onSave = model::save,
        onCancel = onCancel, onRetry = model::load)
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
