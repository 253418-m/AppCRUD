package com.upchiapas.ricetta.presentation.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upchiapas.ricetta.R
import com.upchiapas.ricetta.domain.model.Dificultad
import com.upchiapas.ricetta.domain.model.Receta
import com.upchiapas.ricetta.presentation.auth.AuthColors
import com.upchiapas.ricetta.presentation.auth.AuthFontFamily

private val cardShape = RoundedCornerShape(24.dp)
private val recipeCardShape = RoundedCornerShape(16.dp)

@Composable
fun RecipeHomeScreen(
    state: RecipeListUiState,
    onOpen: (Int) -> Unit,
    onCreate: () -> Unit,
    onLogout: () -> Unit,
    onRetry: () -> Unit,
    feedback: String? = null,
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { RecipeTopBar() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .background(AuthColors.Background).testTag("recipe_home"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(Modifier.widthIn(max = 350.dp)) {
                    Column(Modifier.fillMaxWidth().clip(recipeCardShape)
                        .background(AuthColors.Green).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(AuthColors.Beige), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.auth_book), null,
                                    tint = AuthColors.Green, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("RICETTA", color = AuthColors.Mint, fontSize = 10.sp,
                                    fontFamily = AuthFontFamily)
                                Text("Mis recetas", color = Color.White, fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold, fontFamily = AuthFontFamily)
                            }
                            TextButton(onClick = onLogout,
                                colors = ButtonDefaults.textButtonColors(containerColor = AuthColors.SoftGreen),
                                modifier = Modifier.testTag("recipe_logout")) {
                                Icon(painterResource(R.drawable.auth_logout), null,
                                    tint = AuthColors.Green, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cerrar sesión", color = AuthColors.Green,
                                    fontFamily = AuthFontFamily, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Tu recetario personal", color = Color.White,
                            fontFamily = AuthFontFamily, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(if (state.loaded && state.error == null)
                            "${state.recipes.size} ${if (state.recipes.size == 1) "receta guardada" else "recetas guardadas"}"
                            else "Tus recetas, en un solo lugar.",
                            color = AuthColors.Mint, fontSize = 12.sp, fontFamily = AuthFontFamily)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(Modifier.width(154.dp)) {
                            RecipePrimaryButton("＋ Nueva receta", onCreate, tag = "create_recipe")
                        }
                    }
                    if (feedback != null) {
                        Spacer(Modifier.height(12.dp))
                        SuccessFeedback(feedback)
                    }
                }
            }
            if (state.isLoading) item {
                CircularProgressIndicator(Modifier.testTag("recipe_list_loading"), color = AuthColors.Green)
            } else if (state.error != null) item {
                Box(Modifier.widthIn(max = 350.dp)) {
                    RecipeError(state.error.recipeMessage(), onRetry)
                }
            } else if (state.recipes.isEmpty()) item {
                Column(Modifier.widthIn(max = 350.dp).fillMaxWidth()
                    .clip(cardShape).background(Color.White).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(36.dp))
                        .background(AuthColors.SoftGreen), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.auth_book), null,
                            tint = AuthColors.Green, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Aún no tienes recetas", color = AuthColors.Green, fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold, fontFamily = AuthFontFamily)
                    Spacer(Modifier.height(8.dp))
                    Text("Agrega tu primera receta para comenzar tu cuaderno culinario artesanal.",
                        color = AuthColors.Body, fontSize = 14.sp, fontFamily = AuthFontFamily)
                    Spacer(Modifier.height(16.dp))
                    RecipePrimaryButton("Crear receta", onCreate, tag = "empty_create")
                }
            } else items(state.recipes, key = { it.id }) { recipe ->
                RecipeListCard(recipe, onClick = { onOpen(recipe.id) })
            }
        }
    }
}

@Composable
private fun RecipeListCard(recipe: Receta, onClick: () -> Unit) {
    Row(Modifier.widthIn(max = 350.dp).fillMaxWidth().heightIn(min = 108.dp)
        .clip(recipeCardShape).background(Color.White).clickable(onClick = onClick)
        .padding(16.dp).testTag("recipe_item_${recipe.id}"),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(AuthColors.Beige),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.auth_book), null,
                tint = AuthColors.Green, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(recipe.nombre, color = AuthColors.Ink, fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = AuthFontFamily)
            Spacer(Modifier.height(4.dp))
            if (recipe.esVegetariana) {
                Text("Vegetariana", color = AuthColors.Green,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(AuthColors.SoftGreen).padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp, fontFamily = AuthFontFamily)
                Spacer(Modifier.height(4.dp))
            }
            Text("${recipe.tiempoPreparacionMin} min  ·  ${recipe.porciones} porciones",
                color = AuthColors.Body, fontSize = 11.sp, fontFamily = AuthFontFamily)
            Text(recipe.dificultad.label(), color = AuthColors.Body,
                fontSize = 11.sp, fontFamily = AuthFontFamily)
        }
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
            .background(AuthColors.Beige), contentAlignment = Alignment.Center) {
            Text("›", color = AuthColors.Green, fontSize = 22.sp)
        }
    }
}

@Composable
fun RecipeDetailScreen(
    state: RecipeDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    feedback: String? = null,
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { RecipeTopBar(onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .background(AuthColors.Background).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp).testTag("recipe_detail"),
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.isLoading) {
                CircularProgressIndicator(color = AuthColors.Green)
            } else if (state.error != null) {
                RecipeError(state.error.recipeMessage(), onRetry)
            } else state.recipe?.let { recipe ->
                Column(Modifier.widthIn(max = 350.dp)) {
                    if (feedback != null) {
                        SuccessFeedback(feedback)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("CUADERNO CULINARIO  ·  No. ${recipe.id.toString().padStart(2, '0')}",
                        color = AuthColors.Green, fontSize = 12.sp, fontFamily = AuthFontFamily)
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.fillMaxWidth().clip(cardShape).background(Color.White)
                        .padding(24.dp)) {
                        Text(recipe.nombre, color = AuthColors.Green, fontSize = 24.sp,
                            fontWeight = FontWeight.Bold, fontFamily = AuthFontFamily)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailProperty("Porciones", "${recipe.porciones} raciones", Modifier.weight(1f))
                            DetailProperty("Tiempo", "${recipe.tiempoPreparacionMin} min", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailProperty("Dificultad", recipe.dificultad.label(), Modifier.weight(1f))
                            DetailProperty("Vegetariana", if (recipe.esVegetariana) "Sí" else "No", Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    DetailTextCard("Ingredientes", "Base para ${recipe.porciones} personas",
                        recipe.ingredientes, "Sin ingredientes registrados")
                    Spacer(Modifier.height(16.dp))
                    PreparationCard(recipe.preparacion)
                    Spacer(Modifier.height(24.dp))
                    RecipePrimaryButton("Editar receta", onEdit, tag = "edit_recipe")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onDelete, shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuthColors.Beige),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delete_recipe")) {
                        Text("Eliminar receta", color = AuthColors.Required)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailProperty(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(AuthColors.Field).padding(12.dp)) {
        Text(label, color = AuthColors.Muted, fontSize = 11.sp, fontFamily = AuthFontFamily)
        Text(value, color = AuthColors.Ink, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = AuthFontFamily)
    }
}

@Composable
private fun DetailTextCard(title: String, subtitle: String, value: String?, missing: String) {
    Column(Modifier.fillMaxWidth().clip(cardShape).background(Color.White).padding(24.dp)) {
        Text(title, color = AuthColors.Green, fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = AuthFontFamily)
        Text(subtitle, color = AuthColors.Muted, fontSize = 12.sp, fontFamily = AuthFontFamily)
        Spacer(Modifier.height(16.dp))
        Text(value ?: missing, color = AuthColors.Body, fontSize = 14.sp,
            lineHeight = 22.sp, fontFamily = AuthFontFamily,
            modifier = Modifier.fillMaxWidth().clip(recipeCardShape)
                .background(AuthColors.Field).padding(16.dp))
    }
}

@Composable
private fun PreparationCard(value: String?) {
    Column(Modifier.fillMaxWidth().clip(cardShape).background(Color.White).padding(24.dp)) {
        Text("Preparación", color = AuthColors.Green, fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = AuthFontFamily)
        Text("Guía paso a paso", color = AuthColors.Muted, fontSize = 12.sp,
            fontFamily = AuthFontFamily)
        Spacer(Modifier.height(16.dp))
        if (value == null) Text("Sin preparación registrada",
            color = AuthColors.Body, fontFamily = AuthFontFamily)
        else value.lines().filter { it.isNotBlank() }.forEachIndexed { index, step ->
            Row(Modifier.padding(bottom = 14.dp)) {
                Text((index + 1).toString().padStart(2, '0'), color = AuthColors.Green, fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp))
                Text(step, color = AuthColors.Body, fontSize = 14.sp,
                    lineHeight = 22.sp, fontFamily = AuthFontFamily)
            }
        }
    }
}

@Composable
fun RecipeFormScreen(
    state: RecipeFormUiState, editing: Boolean,
    onNombre: (String) -> Unit, onPorciones: (String) -> Unit,
    onTiempo: (String) -> Unit, onDificultad: (Dificultad) -> Unit,
    onVegetariana: (Boolean) -> Unit, onIngredientes: (String) -> Unit,
    onPreparacion: (String) -> Unit, onSave: () -> Unit, onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    val enabled = !state.isLoading && !state.isSaving
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { RecipeTopBar(onBack = onCancel) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .background(AuthColors.Background).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp).testTag("recipe_form"),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 350.dp)) {
                Text(if (editing) "Editar receta" else "Nueva receta",
                    color = AuthColors.Ink, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, fontFamily = AuthFontFamily)
                Spacer(Modifier.height(8.dp))
                Text(if (editing) "Actualiza los datos de tu receta."
                    else "Registra los datos de tu preparación con precisión y cuidado.",
                    color = AuthColors.Body, fontSize = 14.sp, fontFamily = AuthFontFamily)
                Spacer(Modifier.height(24.dp))
                if (state.isLoading) {
                    CircularProgressIndicator(color = AuthColors.Green)
                } else {
                    if (state.error != null && editing && state.nombre.isEmpty()) {
                        RecipeError(state.error.recipeMessage(), onRetry)
                        Spacer(Modifier.height(16.dp))
                    }
                    Column(Modifier.fillMaxWidth().clip(cardShape).background(Color.White)
                        .padding(20.dp)) {
                        RecipeInput("Nombre de la receta *", state.nombre, onNombre, "form_nombre",
                            placeholder = "Ej. Enchiladas verdes", error = state.errors.nombre,
                            enabled = enabled)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                RecipeInput("Porciones *", state.porciones, onPorciones,
                                    "form_porciones", "Ej. 4", state.errors.porciones,
                                    keyboardType = KeyboardType.Number, enabled = enabled)
                            }
                            Box(Modifier.weight(1f)) {
                                RecipeInput("Tiempo (min) *", state.tiempo, onTiempo,
                                    "form_tiempo", "Ej. 30", state.errors.tiempo,
                                    keyboardType = KeyboardType.Number, enabled = enabled)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Dificultad", color = AuthColors.Body, fontSize = 12.sp,
                            fontFamily = AuthFontFamily)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(AuthColors.Field).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Dificultad.entries.forEach { difficulty ->
                                val selected = state.dificultad == difficulty
                                Box(Modifier.weight(1f).heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) AuthColors.DialogGreen else Color.Transparent)
                                    .clickable(enabled = enabled) { onDificultad(difficulty) }
                                    .testTag("difficulty_${difficulty.name}"),
                                    contentAlignment = Alignment.Center) {
                                    Text(difficulty.label(), color = if (selected) Color.White else AuthColors.Body,
                                        fontSize = 12.sp, fontFamily = AuthFontFamily)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(AuthColors.Field).padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Vegetariana", color = AuthColors.Ink, fontSize = 14.sp,
                                    fontFamily = AuthFontFamily)
                                Text(if (state.esVegetariana) "Sí" else "No", color = AuthColors.Muted,
                                    fontSize = 11.sp, fontFamily = AuthFontFamily)
                            }
                            Switch(checked = state.esVegetariana, onCheckedChange = onVegetariana,
                                enabled = enabled, modifier = Modifier.testTag("form_vegetariana"))
                        }
                        Spacer(Modifier.height(24.dp))
                        RecipeInput("Ingredientes *", state.ingredientes, onIngredientes,
                            "form_ingredientes", "Escribe los ingredientes necesarios...",
                            state.errors.ingredientes, minHeight = 126, singleLine = false,
                            enabled = enabled)
                        Spacer(Modifier.height(24.dp))
                        RecipeInput("Preparación *", state.preparacion, onPreparacion,
                            "form_preparacion", "Describe cómo preparar la receta...",
                            state.errors.preparacion, minHeight = 150, singleLine = false,
                            enabled = enabled)
                        if (state.error != null && (state.nombre.isNotEmpty() || !editing)) {
                            Spacer(Modifier.height(16.dp))
                            RecipeError(state.error.recipeMessage())
                        }
                        Spacer(Modifier.height(32.dp))
                        RecipePrimaryButton(if (editing) "Guardar cambios" else "Guardar receta",
                            onSave, enabled = enabled, tag = "save_recipe")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onCancel, enabled = enabled,
                            modifier = Modifier.fillMaxWidth().testTag("cancel_recipe")) {
                            Text("Cancelar", color = AuthColors.Green)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessFeedback(message: String) {
    Text(message, color = Color.White, fontFamily = AuthFontFamily,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(AuthColors.DialogGreen).padding(16.dp).testTag("recipe_feedback"))
}

fun Dificultad.label(): String = when (this) {
    Dificultad.FACIL -> "Fácil"
    Dificultad.MEDIA -> "Media"
    Dificultad.DIFICIL -> "Difícil"
}
