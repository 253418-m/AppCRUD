package com.upchiapas.ricetta.presentation.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upchiapas.ricetta.R
import com.upchiapas.ricetta.presentation.auth.*
import com.upchiapas.ricetta.presentation.auth.components.*

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onNombreChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onLogin: () -> Unit,
) {
    Scaffold(containerColor = AuthColors.Background, contentWindowInsets = WindowInsets.safeDrawing) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).testTag("register_screen")
        ) {
            Row(
                Modifier.fillMaxWidth().height(80.dp).background(AuthColors.Green).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLogin, enabled = !state.isLoading,
                    modifier = Modifier.size(48.dp).testTag("register_back")) {
                    Icon(painterResource(R.drawable.auth_back), "Volver a iniciar sesión",
                        tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text("RICETTA", color = Color.White, fontFamily = AuthFontFamily,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = .8.sp,
                    modifier = Modifier.padding(start = 4.dp))
            }
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 37.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RegisterHeader()
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.widthIn(max = 350.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp
                ) {
                    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)) {
                        AuthField("Nombre completo", state.nombre, onNombreChange, "Ej. María González",
                            "register_name", state.nombreError, enabled = !state.isLoading,
                            required = true, bordered = true, keyboardType = KeyboardType.Text)
                        Spacer(Modifier.height(14.dp))
                        AuthField("Correo electrónico", state.correo, onCorreoChange, "correo@ejemplo.com",
                            "register_email", state.correoError, enabled = !state.isLoading,
                            required = true, bordered = true, keyboardType = KeyboardType.Email)
                        Spacer(Modifier.height(14.dp))
                        AuthField("Contraseña", state.password, onPasswordChange, "••••••••",
                            "register_password", state.passwordError, enabled = !state.isLoading,
                            required = true, bordered = true, password = true,
                            passwordVisible = state.isPasswordVisible,
                            onToggleVisibility = onTogglePasswordVisibility, toggleTag = "register_toggle_password",
                            helper = "Mínimo 8 caracteres", keyboardType = KeyboardType.Password)
                        Spacer(Modifier.height(14.dp))
                        AuthField("Confirmar contraseña", state.confirmPassword, onConfirmPasswordChange, "••••••••",
                            "register_confirm", state.confirmPasswordError, enabled = !state.isLoading,
                            required = true, bordered = true, password = true,
                            passwordVisible = state.isConfirmPasswordVisible,
                            onToggleVisibility = onToggleConfirmPasswordVisibility, toggleTag = "register_toggle_confirm",
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, onSubmit = onSubmit)
                        state.errorMessage?.let {
                            Spacer(Modifier.height(16.dp))
                            AuthErrorMessage(it)
                        }
                        Spacer(Modifier.height(22.dp))
                        AuthSubmitButton("CREAR CUENTA", state.isLoading, onSubmit, "register_submit")
                        Spacer(Modifier.height(12.dp))
                        AuthFooter("¿Ya tienes una cuenta?", "Iniciar sesión", onLogin,
                            "register_login", compact = true, enabled = !state.isLoading)
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterHeader() {
    Box(
        Modifier.widthIn(max = 350.dp).fillMaxWidth().height(122.dp)
            .clip(RoundedCornerShape(16.dp)).background(AuthColors.Green)
    ) {
        Box(
            Modifier.size(112.dp).align(Alignment.BottomEnd).offset(x = 16.dp, y = 16.dp)
                .clip(RoundedCornerShape(56.dp)).background(Color.White.copy(alpha = .05f))
        )
        Column(Modifier.padding(start = 24.dp, top = 26.dp, end = 24.dp)) {
            Text("Crear cuenta", color = Color.White, fontFamily = AuthFontFamily,
                fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text("Comienza a organizar\ntus recetas", color = AuthColors.Mint,
                fontFamily = AuthFontFamily, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
