package com.emm.justchill.hh.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(onBack: () -> Unit, snackbarHostState: SnackbarHostState, vm: AuthViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AuthEffect.NavigateBack -> currentOnBack()
                is AuthEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is AuthEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AuthContent(
        state = state,
        onIntent = vm::onIntent,
    )
}

@Composable
private fun AuthContent(state: AuthUiState, onIntent: (AuthIntent) -> Unit) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    val submitLabel = if (state.mode == AuthMode.SignIn) "Iniciar sesión" else "Crear cuenta"
    val toggleLabel = if (state.mode == AuthMode.SignIn) {
        "¿No tienes cuenta? Créala"
    } else {
        "¿Ya tienes cuenta? Inicia sesión"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        JcTopBar(
            title = "Tu cuenta",
            left = {
                IconBtn(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = { onIntent(AuthIntent.Back) },
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
        ) {
            Spacer(Modifier.height(spacing.s6))

            AuthFieldInput(
                label = "Correo",
                value = state.email,
                onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
                placeholder = "hola@ejemplo.com",
                keyboardType = KeyboardType.Email,
            )

            Spacer(Modifier.height(spacing.s5))

            AuthFieldInput(
                label = "Contraseña",
                value = state.password,
                onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
                placeholder = "••••••••",
                keyboardType = KeyboardType.Password,
                isPassword = true,
            )

            Spacer(Modifier.height(spacing.s8))

            Text(
                text = toggleLabel,
                style = type.bodyM,
                color = colors.accent,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onIntent(AuthIntent.ToggleMode) }
                    .padding(vertical = spacing.s2),
            )

            Spacer(Modifier.height(spacing.s4))
        }

        StickyCTA(
            label = submitLabel,
            tone = CtaTone.Accent,
            enabled = !state.isLoading,
            onClick = { onIntent(AuthIntent.Submit) },
        )
    }
}

@Composable
private fun AuthFieldInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    val colors = LocalEmmColors.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = colors.textTertiary,
            letterSpacing = 0.4.sp,
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                fontFamily = InterFontFamily,
            ),
            cursorBrush = SolidColor(colors.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = colors.border,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                    )
                }
                .padding(vertical = 8.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W400,
                            fontFamily = InterFontFamily,
                            color = colors.textTertiary,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
