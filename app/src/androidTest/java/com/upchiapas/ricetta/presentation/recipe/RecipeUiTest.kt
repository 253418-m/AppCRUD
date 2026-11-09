package com.upchiapas.ricetta.presentation.recipe

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.upchiapas.ricetta.domain.model.Dificultad
import com.upchiapas.ricetta.domain.model.Receta
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.presentation.theme.RicettaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val sampleRecipe = Receta(4, "Sopa de prueba", 3, 25, Dificultad.MEDIA, true,
    "Agua\nSal", "Hervir\nServir")

class RecipeUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun homeShowsEmptyOnlyForSuccessfulEmptyResponseAndOpensCreate() {
        var createCalls = 0
        val state = mutableStateOf(RecipeListUiState(isLoading = false, loaded = true))
        compose.setContent { RicettaTheme {
            RecipeHomeScreen(state.value, {}, { createCalls++ }, {}, {})
        } }
        compose.onNodeWithText("Aún no tienes recetas").assertIsDisplayed()
        compose.onNodeWithTag("empty_create").performClick()
        assertEquals(1, createCalls)
        state.value = RecipeListUiState(isLoading = false, loaded = true, error = DataError.Timeout)
        compose.onNodeWithText("Aún no tienes recetas").assertDoesNotExist()
        compose.onNodeWithTag("recipe_retry").assertIsDisplayed()
    }

    @Test fun homeUsesLazyListOfRealRecipesAndOpensSelectedId() {
        var opened = 0
        compose.setContent { RicettaTheme {
            RecipeHomeScreen(RecipeListUiState(isLoading = false,
                recipes = listOf(sampleRecipe), loaded = true),
                { opened = it }, {}, {}, {})
        } }
        compose.onNodeWithTag("recipe_item_4").assertIsDisplayed().performClick()
        assertEquals(4, opened)
        compose.onNodeWithText("Sopa de prueba").assertIsDisplayed()
    }

    @Test fun detailShowsPersistedMultilineDataAndHistoricalAbsence() {
        val state = mutableStateOf(RecipeDetailUiState(isLoading = false, recipe = sampleRecipe))
        compose.setContent { RicettaTheme {
            RecipeDetailScreen(state.value, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Sopa de prueba").assertIsDisplayed()
        compose.onNodeWithText("Agua\nSal").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hervir").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Servir").performScrollTo().assertIsDisplayed()
        state.value = RecipeDetailUiState(isLoading = false,
            recipe = sampleRecipe.copy(ingredientes = null, preparacion = null))
        compose.onNodeWithText("Sin ingredientes registrados").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sin preparación registrada").performScrollTo().assertIsDisplayed()
    }

    @Test fun createAndEditUseSameInteractiveForm() {
        val state = mutableStateOf(RecipeFormUiState())
        val editing = mutableStateOf(false)
        var saveCalls = 0
        compose.setContent { RicettaTheme {
            RecipeFormScreen(state.value, editing = editing.value,
                onNombre = { state.value = state.value.copy(nombre = it) },
                onPorciones = { state.value = state.value.copy(porciones = it) },
                onTiempo = { state.value = state.value.copy(tiempo = it) },
                onDificultad = { state.value = state.value.copy(dificultad = it) },
                onVegetariana = { state.value = state.value.copy(esVegetariana = it) },
                onIngredientes = { state.value = state.value.copy(ingredientes = it) },
                onPreparacion = { state.value = state.value.copy(preparacion = it) },
                onSave = { saveCalls++ }, onCancel = {}, onRetry = {})
        } }
        compose.onNodeWithTag("form_nombre").performTextInput("Sopa")
        compose.onNodeWithTag("form_porciones").performTextInput("3")
        compose.onNodeWithTag("form_tiempo").performTextInput("25")
        compose.onNodeWithTag("difficulty_MEDIA").performScrollTo().performClick()
        compose.onNodeWithTag("form_vegetariana").performScrollTo().performClick()
        compose.onNodeWithTag("form_ingredientes").performScrollTo().performTextInput("Agua\nSal")
        compose.onNodeWithTag("form_preparacion").performScrollTo().performTextInput("Hervir\nServir")
        compose.onNodeWithTag("save_recipe").performScrollTo().performClick()
        assertEquals(1, saveCalls)
        assertEquals(Dificultad.MEDIA, state.value.dificultad)
        assertTrue(state.value.esVegetariana)
        assertEquals("Agua\nSal", state.value.ingredientes)
        state.value = state.value.copy(nombre = "Sopa editada")
        editing.value = true
        compose.onNodeWithText("Editar receta").performScrollTo().assertIsDisplayed()
    }

    @Test fun deleteDialogCancelDoesNotConfirmAndConfirmDoes() {
        var canceled = false
        var confirmed = false
        compose.setContent { RicettaTheme {
            RecipeDeleteDialog(RecipeDetailUiState(showDeleteDialog = true,
                recipe = sampleRecipe), { canceled = true }, { confirmed = true })
        } }
        compose.onNodeWithText("¿Eliminar receta?").assertIsDisplayed()
        compose.onNodeWithTag("cancel_delete").performClick()
        assertTrue(canceled)
        assertFalse(confirmed)
        compose.onNodeWithTag("confirm_delete").performClick()
        assertTrue(confirmed)
    }
}
