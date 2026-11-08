package com.upchiapas.ricetta.presentation.auth.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upchiapas.ricetta.R
import com.upchiapas.ricetta.presentation.auth.AuthColors
import com.upchiapas.ricetta.presentation.auth.AuthFontFamily

@Composable
fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    tag: String,
    error: String? = null,
    enabled: Boolean = true,
    required: Boolean = false,
    bordered: Boolean = false,
    @DrawableRes leadingIcon: Int? = null,
    password: Boolean = false,
    passwordVisible: Boolean = false,
    onToggleVisibility: () -> Unit = {},
    toggleTag: String = "",
    helper: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onSubmit: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(12.dp)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = AuthColors.Body, fontFamily = AuthFontFamily,
                fontSize = 12.sp, lineHeight = 16.sp)
            if (required) Text("*", color = AuthColors.Required, fontFamily = AuthFontFamily,
                fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(if (bordered) 4.dp else 6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = AuthColors.Body, fontFamily = AuthFontFamily,
                fontSize = 14.sp, lineHeight = 20.sp),
            visualTransformation = if (password && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            cursorBrush = SolidColor(AuthColors.Green),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) },
                onDone = { onSubmit() },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AuthColors.Field)
                .then(if (bordered || error != null) Modifier.border(
                    if (error != null) 1.dp else 2.dp,
                    if (error != null) AuthColors.Error else AuthColors.FieldBorder, shape
                ) else Modifier)
                .testTag(tag)
                .semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
            decorationBox = { innerTextField ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .padding(start = 16.dp, end = if (password) 0.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(painterResource(leadingIcon), null,
                            tint = AuthColors.Body, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(
                            placeholder, color = if (bordered) AuthColors.Muted else AuthColors.Placeholder,
                            fontFamily = AuthFontFamily, fontSize = 14.sp, lineHeight = 20.sp
                        )
                        innerTextField()
                    }
                    if (password) {
                        IconButton(
                            onClick = onToggleVisibility, enabled = enabled,
                            modifier = Modifier.size(48.dp).testTag(toggleTag)
                        ) {
                            Icon(
                                painterResource(R.drawable.auth_eye),
                                contentDescription = if (passwordVisible)
                                    "Ocultar $label" else "Mostrar $label",
                                tint = if (bordered) AuthColors.Muted else AuthColors.Body,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        )
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, color = AuthColors.Error, fontFamily = AuthFontFamily,
                fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } else if (helper != null) {
            Spacer(Modifier.height(4.dp))
            Text(helper, color = AuthColors.Muted, fontFamily = AuthFontFamily,
                fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun AuthSubmitButton(
    label: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    tag: String,
    showArrow: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthColors.Green, contentColor = Color.White,
            disabledContainerColor = AuthColors.Green.copy(alpha = .65f),
            disabledContentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag(tag)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                Modifier.size(18.dp).semantics { contentDescription = "Procesando" },
                color = Color.White, strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(label, fontFamily = AuthFontFamily, fontWeight = FontWeight.Medium,
            fontSize = 14.sp, letterSpacing = .2.sp)
        if (showArrow && !isLoading) {
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.auth_arrow), null, Modifier.size(16.dp))
        }
    }
}

@Composable
fun AuthErrorMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        message, color = AuthColors.Error, fontFamily = AuthFontFamily,
        fontSize = 13.sp, lineHeight = 20.sp,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(AuthColors.ErrorContainer).padding(12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
fun AuthFooter(
    question: String,
    action: String,
    onClick: () -> Unit,
    tag: String,
    compact: Boolean = false,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(question, color = AuthColors.Body, fontFamily = AuthFontFamily,
            fontSize = if (compact) 12.sp else 14.sp,
            modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(6.dp))
        Text(
            action, color = AuthColors.Green, fontFamily = AuthFontFamily,
            fontSize = if (compact) 14.sp else 16.sp,
            fontWeight = if (compact) FontWeight.SemiBold else FontWeight.Normal,
            textDecoration = if (compact) null else TextDecoration.Underline,
            modifier = Modifier.heightIn(min = 48.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(vertical = 14.dp).testTag(tag)
        )
    }
}
