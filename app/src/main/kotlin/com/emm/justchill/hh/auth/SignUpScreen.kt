package com.emm.justchill.hh.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun SignUpScreen(
    state: SignUpUiState = SignUpUiState(),
    onAction: (SignUpAction) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val keyboard = LocalSoftwareKeyboardController.current

    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.s4),
    ) {

        Spacer(Modifier.height(spacing.s4))

        BackChevron(onBack = onBack)

        Spacer(Modifier.height(spacing.s8))

        Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            Text(
                text = "Crea tu cuenta",
                style = type.display,
                color = colors.textPrimary,
            )
            Text(
                text = "Únete a JustChill",
                style = type.bodyM,
                color = colors.textSecondary,
            )
        }

        Spacer(Modifier.height(spacing.s8))

        EmmTextInput(
            value = state.email,
            onValueChange = { onAction(SignUpAction.OnEmailChange(it)) },
            label = "CORREO",
            placeholder = "tu@correo.com",
            keyboardType = KeyboardType.Email,
            isError = state.emailError != null,
            helper = state.emailError,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(spacing.s5))

        EmmTextInput(
            value = state.password,
            onValueChange = { onAction(SignUpAction.OnPasswordChange(it)) },
            label = "CONTRASEÑA",
            placeholder = "••••••••",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            isError = state.passwordError != null,
            helper = state.passwordError,
            trailingContent = {
                VisibilityToggle(showing = showPassword) { showPassword = !showPassword }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(spacing.s5))

        EmmTextInput(
            value = state.confirmPassword,
            onValueChange = { onAction(SignUpAction.OnConfirmPasswordChange(it)) },
            label = "REPITE LA CONTRASEÑA",
            placeholder = "••••••••",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingContent = {
                VisibilityToggle(showing = showConfirm) { showConfirm = !showConfirm }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(spacing.s6))

        TermsCheckbox(
            isChecked = state.isChecked,
            onToggle = { onAction(SignUpAction.OnCheckedChange(!state.isChecked)) },
        )

        Spacer(Modifier.height(spacing.s6))

        EmmButton(
            text = "Crear cuenta",
            onClick = {
                keyboard?.hide()
                onAction(SignUpAction.SignUp)
            },
            enabled = state.isValidFields,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.error != null) {
            Spacer(Modifier.height(spacing.s3))
            Text(
                text = state.error,
                style = type.bodyM,
                color = colors.danger,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(spacing.s8))

        Footer(onBack = onBack)

        Spacer(Modifier.height(spacing.s8))
    }
}

@Composable
private fun BackChevron(onBack: () -> Unit) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onBack,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = "Atrás",
            tint = colors.textPrimary,
            modifier = Modifier.size(24.dp),
        )
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
private fun TermsCheckbox(isChecked: Boolean, onToggle: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (isChecked) colors.accent else colors.bg,
                    radii.rS,
                )
                .border(1.dp, if (isChecked) colors.accent else colors.border, radii.rS),
            contentAlignment = Alignment.Center,
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.textOnAccent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Text(
            text = "Acepto los términos y condiciones",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun Footer(onBack: () -> Unit) {
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
            text = "¿Ya tienes una cuenta? ",
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Text(
            text = "Inicia sesión",
            style = type.labelL,
            color = colors.textPrimary,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onBack,
                )
                .padding(spacing.s1),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 1000)
@Composable
private fun SignUpScreenPreview() {
    EmmTheme {
        SignUpScreen(
            state = SignUpUiState(
                email = "luke.trevino@example.com",
                password = "123456",
                confirmPassword = "123456",
                isValidFields = true,
                isChecked = true,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 1000)
@Composable
private fun SignUpScreenErrorPreview() {
    EmmTheme {
        SignUpScreen(
            state = SignUpUiState(
                email = "luke",
                emailError = "Correo no válido",
                password = "123",
                passwordError = "Mínimo 6 caracteres",
                confirmPassword = "1234",
            ),
        )
    }
}
