package com.upchiapas.ricetta.presentation.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.upchiapas.ricetta.R
import com.upchiapas.ricetta.presentation.auth.AuthColors
import com.upchiapas.ricetta.presentation.auth.AuthFontFamily

@Composable
fun LogoutConfirmation(
    isLoading: Boolean,
    errorMessage: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onCancel() },
        modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 350.dp)
            .fillMaxWidth().testTag("logout_dialog"),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        textContentColor = AuthColors.Body,
        text = {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AuthColors.Beige),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.auth_profile), null,
                        tint = AuthColors.DialogGreen, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("¿Cerrar sesión?", color = AuthColors.DialogGreen,
                        fontFamily = AuthFontFamily, fontSize = 18.sp,
                        lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tendrás que iniciar sesión nuevamente para acceder a tus recetas guardadas y notas culinarias.",
                        color = AuthColors.Body, fontFamily = AuthFontFamily,
                        fontSize = 13.sp, lineHeight = 21.sp
                    )
                    errorMessage?.let {
                        Spacer(Modifier.height(12.dp))
                        AuthErrorMessage(it)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isLoading,
                modifier = Modifier.testTag("logout_cancel")) {
                Text("Cancelar", color = Color(0xFF4B6548), fontFamily = AuthFontFamily,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm, enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuthColors.SoftGreen, contentColor = AuthColors.SoftGreenText,
                    disabledContainerColor = AuthColors.SoftGreen.copy(alpha = .65f),
                    disabledContentColor = AuthColors.SoftGreenText,
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.heightIn(min = 44.dp).testTag("logout_confirm"),
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp),
                    color = AuthColors.SoftGreenText, strokeWidth = 2.dp)
                else Icon(painterResource(R.drawable.auth_logout), null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cerrar sesión", fontFamily = AuthFontFamily, fontSize = 14.sp,
                    fontWeight = FontWeight.Medium)
            }
        }
    )
}
