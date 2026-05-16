package com.emm.justchill.hh.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
    navigateToRegister: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.s4),
    ) {

        Spacer(Modifier.height(spacing.s12))

        Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            Text(
                text = "Bienvenido",
                style = type.display,
                color = colors.textPrimary,
            )
            Text(
                text = "Inicia sesión para continuar",
                style = type.bodyM,
                color = colors.textSecondary,
            )
        }

        Spacer(Modifier.height(spacing.s8))

        EmmTextInput(
            value = state.email,
            onValueChange = { onAction(LoginAction.UpdateEmail(it)) },
            label = "CORREO",
            placeholder = "tu@correo.com",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(spacing.s5))

        EmmTextInput(
            value = state.password,
            onValueChange = { onAction(LoginAction.UpdatePassword(it)) },
            label = "CONTRASEÑA",
            placeholder = "••••••••",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingContent = {
                VisibilityToggle(showing = showPassword) { showPassword = !showPassword }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(spacing.s3))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            ClickableText(
                text = "¿Olvidaste tu contraseña?",
                onClick = { /* TODO: link to forgot flow */ },
                color = colors.textTertiary,
            )
        }

        Spacer(Modifier.height(spacing.s6))

        EmmButton(
            text = "Iniciar sesión",
            onClick = { onAction(LoginAction.Login) },
            enabled = state.isValidFields,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.errorMsg != null) {
            Spacer(Modifier.height(spacing.s3))
            Text(
                text = state.errorMsg,
                style = type.bodyM,
                color = colors.danger,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(spacing.s8))

        Footer(onRegister = navigateToRegister)

        Spacer(Modifier.height(spacing.s12))
    }
}

@Composable
private fun VisibilityToggle(showing: Boolean, onToggle: () -> Unit) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (showing) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            contentDescription = if (showing) "Ocultar contraseña" else "Mostrar contraseña",
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ClickableText(
    text: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color,
) {
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        style = type.labelL,
        color = color,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(spacing.s2),
    )
}

@Composable
private fun Footer(onRegister: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "¿No tienes una cuenta? ",
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Text(
            text = "Regístrate",
            style = type.labelL,
            color = colors.textPrimary,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onRegister,
                )
                .padding(spacing.s1),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    EmmTheme {
        LoginScreen(
            state = LoginUiState(
                email = "allen.waller@example.com",
                password = "123456",
                isLoading = false,
                isValidFields = true,
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun LoginScreenLoadingPreview() {
    EmmTheme {
        LoginScreen(
            state = LoginUiState(
                email = "allen.waller@example.com",
                password = "123456",
                isLoading = true,
                isValidFields = true,
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun LoginScreenErrorPreview() {
    EmmTheme {
        LoginScreen(
            state = LoginUiState(
                email = "allen.waller@example.com",
                password = "wrong",
                isValidFields = true,
                errorMsg = "Credenciales inválidas. Intenta de nuevo.",
            ),
            onAction = {},
        )
    }
}
