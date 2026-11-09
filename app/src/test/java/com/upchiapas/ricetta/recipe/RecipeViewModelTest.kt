package com.upchiapas.ricetta.recipe

import com.upchiapas.ricetta.auth.MainDispatcherRule
import com.upchiapas.ricetta.domain.model.Dificultad
import com.upchiapas.ricetta.domain.model.GuardarReceta
import com.upchiapas.ricetta.domain.model.Receta
import com.upchiapas.ricetta.domain.repository.RecipeRepository
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.presentation.recipe.RecipeDetailViewModel
import com.upchiapas.ricetta.presentation.recipe.RecipeFormViewModel
import com.upchiapas.ricetta.presentation.recipe.RecipeListViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

private val sample = Receta(7, "Sopa", 2, 30, Dificultad.FACIL, true,
    "Agua\nSal", "Hervir\nServir")

private class FakeRecipes : RecipeRepository {
    var listCalls = 0
    var getCalls = 0
    var createCalls = 0
    var updateCalls = 0
    var deleteCalls = 0
    var lastBody: GuardarReceta? = null
    var list: suspend () -> AppResult<List<Receta>> = { AppResult.Success(emptyList()) }
    var detail: suspend () -> AppResult<Receta> = { AppResult.Success(sample) }
    var create: suspend (GuardarReceta) -> AppResult<Receta> = { AppResult.Success(sample) }
    var update: suspend (GuardarReceta) -> AppResult<Receta> = { AppResult.Success(sample) }
    var delete: suspend () -> AppResult<Unit> = { AppResult.Success(Unit) }
    override suspend fun obtenerRecetas(): AppResult<List<Receta>> {
        listCalls++
        return list()
    }
    override suspend fun obtenerReceta(id: Int): AppResult<Receta> {
        getCalls++
        return detail()
    }
    override suspend fun crearReceta(receta: GuardarReceta): AppResult<Receta> {
        createCalls++
        lastBody = receta
        return create(receta)
    }
    override suspend fun actualizarReceta(id: Int, receta: GuardarReceta): AppResult<Receta> {
        updateCalls++
        lastBody = receta
        return update(receta)
    }
    override suspend fun eliminarReceta(id: Int): AppResult<Unit> {
        deleteCalls++
        return delete()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun emptyListIsDifferentFromFailureAndRetryLoadsRealData() = runTest {
        val repo = FakeRecipes()
        val vm = RecipeListViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertTrue(vm.state.value.recipes.isEmpty())
        assertNull(vm.state.value.error)
        repo.list = { AppResult.Failure(DataError.Timeout) }
        vm.refresh()
        advanceUntilIdle()
        assertEquals(DataError.Timeout, vm.state.value.error)
        repo.list = { AppResult.Success(listOf(sample)) }
        vm.refresh()
        advanceUntilIdle()
        assertEquals(listOf(sample), vm.state.value.recipes)
        assertEquals(3, repo.listCalls)
    }

    @Test fun listPreventsDuplicateActiveRequests() = runTest {
        val pending = CompletableDeferred<AppResult<List<Receta>>>()
        val repo = FakeRecipes().apply { list = { pending.await() } }
        val vm = RecipeListViewModel(repo)
        runCurrent()
        vm.refresh()
        assertEquals(1, repo.listCalls)
        pending.complete(AppResult.Success(listOf(sample)))
        advanceUntilIdle()
        assertEquals(listOf(sample), vm.state.value.recipes)
    }

    @Test fun detailLoadsAndDeleteRequiresConfirmation() = runTest {
        val repo = FakeRecipes()
        val vm = RecipeDetailViewModel(7, repo)
        advanceUntilIdle()
        assertEquals(sample, vm.state.value.recipe)
        vm.requestDelete()
        vm.cancelDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(0, repo.deleteCalls)
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(1, repo.deleteCalls)
        assertTrue(vm.state.value.deleted)
    }

    @Test fun failedDeleteKeepsRecipeAndOffersRetry() = runTest {
        val repo = FakeRecipes().apply { delete = { AppResult.Failure(DataError.Server) } }
        val vm = RecipeDetailViewModel(7, repo)
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(sample, vm.state.value.recipe)
        assertTrue(vm.state.value.showDeleteDialog)
        assertFalse(vm.state.value.deleted)
        assertEquals(DataError.Server, vm.state.value.deleteError)
    }

    @Test fun missingDetailIsReportedAndRetryWorks() = runTest {
        val repo = FakeRecipes().apply { detail = { AppResult.Failure(DataError.NotFound) } }
        val vm = RecipeDetailViewModel(7, repo)
        advanceUntilIdle()
        assertEquals(DataError.NotFound, vm.state.value.error)
        repo.detail = { AppResult.Success(sample) }
        vm.refresh()
        advanceUntilIdle()
        assertEquals(sample, vm.state.value.recipe)
    }

    @Test fun createRejectsBlankAndOverflowWithoutHttp() = runTest {
        val repo = FakeRecipes()
        val vm = RecipeFormViewModel(null, repo)
        vm.save()
        assertTrue(vm.state.value.errors.any)
        vm.onNombre("Sopa")
        vm.onPorciones("2147483648")
        vm.onTiempo("0")
        vm.onIngredientes("  ")
        vm.onPreparacion("")
        vm.save()
        assertNotNull(vm.state.value.errors.porciones)
        assertNotNull(vm.state.value.errors.tiempo)
        assertNotNull(vm.state.value.errors.ingredientes)
        assertNotNull(vm.state.value.errors.preparacion)
        assertEquals(0, repo.createCalls)
    }

    @Test fun createSendsMultilineTextAndSelectionsOnlyAfterExplicitSave() = runTest {
        val repo = FakeRecipes()
        val vm = RecipeFormViewModel(null, repo)
        vm.onNombre("  Sopa  ")
        vm.onPorciones("2")
        vm.onTiempo("30")
        vm.onDificultad(Dificultad.MEDIA)
        vm.onVegetariana(true)
        vm.onIngredientes("Agua\nSal")
        vm.onPreparacion("Hervir\nServir")
        assertEquals(0, repo.createCalls)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.createCalls)
        assertEquals("Sopa", repo.lastBody?.nombre)
        assertEquals("Agua\nSal", repo.lastBody?.ingredientes)
        assertEquals("Hervir\nServir", repo.lastBody?.preparacion)
        assertEquals(Dificultad.MEDIA, repo.lastBody?.dificultad)
        assertTrue(repo.lastBody?.esVegetariana == true)
        assertEquals(sample, vm.state.value.saved)
    }

    @Test fun editingPrefillsRealFieldsIncludingHistoricalMissingTexts() = runTest {
        val repo = FakeRecipes().apply {
            detail = { AppResult.Success(sample.copy(ingredientes = null, preparacion = null)) }
        }
        val vm = RecipeFormViewModel(7, repo)
        advanceUntilIdle()
        assertEquals("Sopa", vm.state.value.nombre)
        assertEquals("2", vm.state.value.porciones)
        assertEquals("", vm.state.value.ingredientes)
        assertEquals("", vm.state.value.preparacion)
        vm.save()
        assertEquals(0, repo.updateCalls)
        vm.onIngredientes("Agua")
        vm.onPreparacion("Hervir")
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.updateCalls)
        assertEquals("Agua", repo.lastBody?.ingredientes)
    }

    @Test fun failedSavePreservesFormAndAllowsRetry() = runTest {
        val repo = FakeRecipes().apply { create = { AppResult.Failure(DataError.Server) } }
        val vm = RecipeFormViewModel(null, repo)
        vm.onNombre("Sopa")
        vm.onPorciones("2")
        vm.onTiempo("30")
        vm.onIngredientes("Agua")
        vm.onPreparacion("Hervir")
        vm.save()
        advanceUntilIdle()
        assertEquals(DataError.Server, vm.state.value.error)
        assertEquals("Sopa", vm.state.value.nombre)
        repo.create = { AppResult.Success(sample) }
        vm.save()
        advanceUntilIdle()
        assertEquals(2, repo.createCalls)
        assertEquals(sample, vm.state.value.saved)
    }

    @Test fun savePreventsDuplicateRequestsWhileLoading() = runTest {
        val pending = CompletableDeferred<AppResult<Receta>>()
        val repo = FakeRecipes().apply { create = { pending.await() } }
        val vm = RecipeFormViewModel(null, repo)
        vm.onNombre("Sopa")
        vm.onPorciones("2")
        vm.onTiempo("30")
        vm.onIngredientes("Agua")
        vm.onPreparacion("Hervir")
        vm.save()
        vm.save()
        runCurrent()
        assertEquals(1, repo.createCalls)
        pending.complete(AppResult.Success(sample))
        advanceUntilIdle()
        assertEquals(sample, vm.state.value.saved)
    }
}
