package com.upchiapas.ricetta.presentation.auth.login

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upchiapas.ricetta.R
import com.upchiapas.ricetta.presentation.auth.*
import com.upchiapas.ricetta.presentation.auth.components.*

@Composable
fun LoginScreen(
    state: LoginUiState,
    onCorreoChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onRegister: () -> Unit,
) {
    Scaffold(containerColor = AuthColors.Background, contentWindowInsets = WindowInsets.safeDrawing) { insets ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)
                .testTag("login_screen")
        ) {
            val viewportHeight = maxHeight
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight).padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 350.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = AuthColors.Background,
                    shadowElevation = 6.dp,
                ) {
                    Column {
                        LoginHeader()
                        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp)) {
                            Text("Iniciar sesión", color = AuthColors.Ink, fontFamily = AuthFontFamily,
                                fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Ingresa tus datos para acceder a tus\nrecetas.", color = AuthColors.Body,
                                fontFamily = AuthFontFamily, fontSize = 14.sp, lineHeight = 22.sp)
                            Spacer(Modifier.height(24.dp))
                            AuthField(
                                label = "Correo electrónico", value = state.correo,
                                onValueChange = onCorreoChange, placeholder = "usuario@ejemplo.com",
                                tag = "login_email", error = state.correoError,
                                enabled = !state.isLoading, leadingIcon = R.drawable.auth_mail,
                                keyboardType = KeyboardType.Email
                            )
                            Spacer(Modifier.height(16.dp))
                            AuthField(
                                label = "Contraseña", value = state.password, onValueChange = onPasswordChange,
                                placeholder = "", tag = "login_password", error = state.passwordError,
                                enabled = !state.isLoading, leadingIcon = R.drawable.auth_lock,
                                password = true, passwordVisible = state.isPasswordVisible,
                                onToggleVisibility = onTogglePasswordVisibility, toggleTag = "login_toggle_password",
                                keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, onSubmit = onSubmit,
                            )
                            state.errorMessage?.let {
                                Spacer(Modifier.height(16.dp))
                                AuthErrorMessage(it)
                            }
                            Spacer(Modifier.height(28.dp))
                            AuthSubmitButton("INICIAR SESIÓN", state.isLoading, onSubmit, "login_submit", showArrow = true)
                            Spacer(Modifier.height(24.dp))
                            AuthFooter("¿No tienes una cuenta?", "Crear cuenta", onRegister, "login_register",
                                enabled = !state.isLoading)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginHeader() {
    Box(
        Modifier.fillMaxWidth().height(214.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(AuthColors.Green)
    ) {
        Icon(
            painterResource(R.drawable.auth_book), null,
            tint = AuthColors.Background,
            modifier = Modifier.size(width = 110.dp, height = 80.dp)
                .align(Alignment.BottomEnd).offset(x = 24.dp, y = 18.dp).alpha(.1f)
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                    .background(AuthColors.Field.copy(alpha = .1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.auth_cutlery),
                    contentDescription = "Utensilios de cocina",
                    tint = AuthColors.Mint,
                    modifier = Modifier.size(32.dp).testTag("login_logo"),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("RICETTA", color = AuthColors.Background, fontFamily = AuthFontFamily,
                fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(3.dp))
            Text("Tus recetas, en un solo lugar", color = AuthColors.Mint, fontFamily = AuthFontFamily,
                fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = .3.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.width(48.dp).height(2.dp).background(AuthColors.Mint.copy(alpha = .3f)))
        }
    }
}
