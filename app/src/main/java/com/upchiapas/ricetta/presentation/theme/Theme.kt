package com.upchiapas.ricetta.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.upchiapas.ricetta.presentation.auth.AuthColors

private val RicettaColors = lightColorScheme(
    primary = AuthColors.Green,
    onPrimary = Color.White,
    secondary = AuthColors.SoftGreen,
    onSecondary = AuthColors.SoftGreenText,
    background = AuthColors.Background,
    onBackground = AuthColors.Ink,
    surface = Color.White,
    onSurface = AuthColors.Ink,
    error = AuthColors.Error,
    onError = Color.White,
    errorContainer = AuthColors.ErrorContainer,
    outline = AuthColors.FieldBorder,
)

// Solo se proporcionaron diseños claros; no aplicar colores dinámicos que alteren los SVG.
@Composable
fun RicettaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RicettaColors, typography = Typography, content = content)
}
