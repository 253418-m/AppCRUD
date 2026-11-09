package com.upchiapas.ricetta.presentation.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upchiapas.ricetta.R
import com.upchiapas.ricetta.presentation.auth.AuthColors
import com.upchiapas.ricetta.presentation.auth.AuthFontFamily

@Composable
fun RecipeTopBar(onBack: (() -> Unit)? = null, onLogout: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(80.dp).background(AuthColors.Green)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("recipe_back")) {
                Icon(painterResource(R.drawable.auth_back), "Regresar",
                    tint = androidx.compose.ui.graphics.Color.White)
            }
        }
        Text("RICETTA", color = androidx.compose.ui.graphics.Color.White,
            fontFamily = AuthFontFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        if (onLogout != null) {
            IconButton(onClick = onLogout, modifier = Modifier.testTag("recipe_logout")) {
                Icon(painterResource(R.drawable.auth_logout), "Cerrar sesión",
                    tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

@Composable
fun RecipePrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true, tag: String) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AuthColors.DialogGreen),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag(tag)) {
        Text(label, fontFamily = AuthFontFamily, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RecipeError(message: String, onRetry: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(AuthColors.ErrorContainer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = AuthColors.Error, fontFamily = AuthFontFamily,
            fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (onRetry != null) TextButton(onClick = onRetry, modifier = Modifier.testTag("recipe_retry")) {
            Text("Reintentar", color = AuthColors.Green)
        }
    }
}

@Composable
fun RecipeInput(
    label: String, value: String, onChange: (String) -> Unit, tag: String,
    placeholder: String = "", error: String? = null,
    minHeight: Int = 48, singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text, enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    Column {
        Text(label, color = AuthColors.Body, fontFamily = AuthFontFamily,
            fontSize = 12.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value, onValueChange = onChange, enabled = enabled,
            singleLine = singleLine,
            textStyle = TextStyle(color = AuthColors.Body, fontFamily = AuthFontFamily,
                fontSize = 14.sp, lineHeight = 20.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(AuthColors.Green),
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp)
                .clip(shape).background(AuthColors.Field)
                .then(if (error != null) Modifier.border(1.dp, AuthColors.Error, shape) else Modifier)
                .testTag(tag).semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().heightIn(min = minHeight.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (value.isEmpty()) Text(placeholder, color = AuthColors.Muted,
                        fontFamily = AuthFontFamily, fontSize = 14.sp)
                    inner()
                }
            }
        )
        if (error != null) Text(error, color = AuthColors.Error,
            fontFamily = AuthFontFamily, fontSize = 12.sp)
    }
}

@Composable
fun RecipeDeleteDialog(state: RecipeDetailUiState, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!state.isDeleting) onCancel() },
        shape = RoundedCornerShape(24.dp),
        containerColor = androidx.compose.ui.graphics.Color.White,
        title = { Text("¿Eliminar receta?", color = AuthColors.Green,
            fontFamily = AuthFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("¿Seguro que deseas eliminar esta receta? Esta acción no se puede deshacer y se borrará de tu recetario permanentemente.",
                    color = AuthColors.Body, fontFamily = AuthFontFamily)
                if (state.deleteError != null) {
                    Spacer(Modifier.height(12.dp))
                    RecipeError(state.deleteError.recipeMessage())
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !state.isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = AuthColors.Required),
                modifier = Modifier.testTag("confirm_delete")) {
                if (state.isDeleting) CircularProgressIndicator(Modifier.size(16.dp),
                    color = androidx.compose.ui.graphics.Color.White)
                else Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !state.isDeleting,
                modifier = Modifier.testTag("cancel_delete")) {
                Text("Cancelar", color = AuthColors.Green)
            }
        },
        modifier = Modifier.testTag("delete_dialog")
    )
}
