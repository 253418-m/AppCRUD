package com.upchiapas.ricetta.presentation.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.upchiapas.ricetta.domain.model.Dificultad
import com.upchiapas.ricetta.domain.model.GuardarReceta
import com.upchiapas.ricetta.domain.model.Receta
import com.upchiapas.ricetta.domain.repository.RecipeRepository
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun DataError.recipeMessage(): String = when (this) {
    DataError.NoInternet -> "No pudimos conectarnos. Revisa tu conexión."
    DataError.Timeout -> "La consulta tardó demasiado. Inténtalo nuevamente."
    DataError.Unauthorized -> "La sesión terminó. Inicia sesión nuevamente."
    DataError.NotFound -> "La receta ya no está disponible."
    DataError.Conflict -> "No se pudo guardar por un conflicto. Inténtalo nuevamente."
    DataError.Validation -> "Revisa los datos de la receta."
    DataError.Server -> "El servicio no está disponible. Inténtalo más tarde."
    DataError.Serialization -> "No pudimos procesar la respuesta."
    DataError.Storage, DataError.Unknown -> "No pudimos completar la operación. Inténtalo nuevamente."
}

data class RecipeListUiState(
    val isLoading: Boolean = true,
    val recipes: List<Receta> = emptyList(),
    val error: DataError? = null,
    val loaded: Boolean = false,
)

class RecipeListViewModel(private val repository: RecipeRepository) : ViewModel() {
    private val _state = MutableStateFlow(RecipeListUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init { refresh() }

    fun refresh() {
        if (loadJob?.isActive == true) return
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                when (val result = repository.obtenerRecetas()) {
                    is AppResult.Success -> _state.value = RecipeListUiState(
                        recipes = result.data, loaded = true, isLoading = false
                    )
                    is AppResult.Failure -> _state.update {
                        it.copy(isLoading = false, loaded = true, error = result.error)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val recipe: Receta? = null,
    val error: DataError? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: DataError? = null,
    val deleted: Boolean = false,
)

class RecipeDetailViewModel(
    private val id: Int,
    private val repository: RecipeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RecipeDetailUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init { refresh() }

    fun refresh() {
        if (_state.value.isDeleting || loadJob?.isActive == true) return
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                when (val result = repository.obtenerReceta(id)) {
                    is AppResult.Success -> _state.update {
                        it.copy(isLoading = false, recipe = result.data, error = null)
                    }
                    is AppResult.Failure -> _state.update {
                        it.copy(isLoading = false, error = result.error)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun requestDelete() {
        if (_state.value.recipe != null && !_state.value.isDeleting)
            _state.update { it.copy(showDeleteDialog = true, deleteError = null) }
    }

    fun cancelDelete() {
        if (!_state.value.isDeleting)
            _state.update { it.copy(showDeleteDialog = false, deleteError = null) }
    }

    fun confirmDelete() {
        if (!_state.value.showDeleteDialog || _state.value.isDeleting) return
        _state.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            try {
                when (val result = repository.eliminarReceta(id)) {
                    is AppResult.Success -> _state.update {
                        it.copy(deleted = true, showDeleteDialog = false)
                    }
                    is AppResult.Failure -> _state.update {
                        it.copy(deleteError = result.error)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                _state.update { it.copy(isDeleting = false) }
            }
        }
    }
}

data class RecipeFormErrors(
    val nombre: String? = null,
    val porciones: String? = null,
    val tiempo: String? = null,
    val ingredientes: String? = null,
    val preparacion: String? = null,
) {
    val any get() = listOf(nombre, porciones, tiempo, ingredientes, preparacion).any { it != null }
}

data class RecipeFormUiState(
    val nombre: String = "",
    val porciones: String = "",
    val tiempo: String = "",
    val dificultad: Dificultad = Dificultad.FACIL,
    val esVegetariana: Boolean = false,
    val ingredientes: String = "",
    val preparacion: String = "",
    val errors: RecipeFormErrors = RecipeFormErrors(),
    val error: DataError? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Receta? = null,
)

class RecipeFormViewModel(
    private val id: Int?,
    private val repository: RecipeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RecipeFormUiState())
    val state = _state.asStateFlow()

    init { if (id != null) load() }

    fun load() {
        val recipeId = id ?: return
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = repository.obtenerReceta(recipeId)) {
                    is AppResult.Success -> _state.update {
                        it.copy(
                            nombre = result.data.nombre,
                            porciones = result.data.porciones.toString(),
                            tiempo = result.data.tiempoPreparacionMin.toString(),
                            dificultad = result.data.dificultad,
                            esVegetariana = result.data.esVegetariana,
                            ingredientes = result.data.ingredientes.orEmpty(),
                            preparacion = result.data.preparacion.orEmpty(),
                            error = null
                        )
                    }
                    is AppResult.Failure -> _state.update { it.copy(error = result.error) }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNombre(value: String) = _state.update {
        it.copy(nombre = value, errors = it.errors.copy(nombre = null), error = null)
    }
    fun onPorciones(value: String) = _state.update {
        it.copy(porciones = value, errors = it.errors.copy(porciones = null), error = null)
    }
    fun onTiempo(value: String) = _state.update {
        it.copy(tiempo = value, errors = it.errors.copy(tiempo = null), error = null)
    }
    fun onDificultad(value: Dificultad) = _state.update { it.copy(dificultad = value) }
    fun onVegetariana(value: Boolean) = _state.update { it.copy(esVegetariana = value) }
    fun onIngredientes(value: String) = _state.update {
        it.copy(ingredientes = value, errors = it.errors.copy(ingredientes = null), error = null)
    }
    fun onPreparacion(value: String) = _state.update {
        it.copy(preparacion = value, errors = it.errors.copy(preparacion = null), error = null)
    }

    fun save() {
        val current = _state.value
        if (current.isLoading || current.isSaving || current.saved != null) return
        val portions = current.porciones.toIntOrNull()
        val minutes = current.tiempo.toIntOrNull()
        val errors = RecipeFormErrors(
            nombre = if (current.nombre.isBlank()) "Escribe el nombre de la receta." else null,
            porciones = if (portions == null || portions < 1) "Ingresa un entero mayor que cero." else null,
            tiempo = if (minutes == null || minutes < 1) "Ingresa un entero mayor que cero." else null,
            ingredientes = if (current.ingredientes.isBlank()) "Escribe los ingredientes." else null,
            preparacion = if (current.preparacion.isBlank()) "Escribe la preparación." else null,
        )
        if (errors.any) {
            _state.update { it.copy(errors = errors) }
            return
        }
        val body = GuardarReceta(
            nombre = current.nombre.trim(), porciones = requireNotNull(portions),
            tiempoPreparacionMin = requireNotNull(minutes), dificultad = current.dificultad,
            esVegetariana = current.esVegetariana, ingredientes = current.ingredientes,
            preparacion = current.preparacion
        )
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val result = if (id == null) repository.crearReceta(body)
                             else repository.actualizarReceta(id, body)
                when (result) {
                    is AppResult.Success -> _state.update { it.copy(saved = result.data) }
                    is AppResult.Failure -> _state.update { it.copy(error = result.error) }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}

object RecipeViewModelFactories {
    fun list(repository: RecipeRepository): ViewModelProvider.Factory = viewModelFactory {
        initializer { RecipeListViewModel(repository) }
    }
    fun detail(id: Int, repository: RecipeRepository): ViewModelProvider.Factory = viewModelFactory {
        initializer { RecipeDetailViewModel(id, repository) }
    }
    fun form(id: Int?, repository: RecipeRepository): ViewModelProvider.Factory = viewModelFactory {
        initializer { RecipeFormViewModel(id, repository) }
    }
}
