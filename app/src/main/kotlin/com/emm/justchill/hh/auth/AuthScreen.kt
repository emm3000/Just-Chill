package com.emm.justchill.hh.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.FilledCta
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.OutlinedCta
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    vm: AuthViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val context = LocalContext.current

    BackHandler(enabled = state is AuthUiState.CheckEmail) { vm.onIntent(AuthIntent.Back) }

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AuthEffect.NavigateBack -> currentOnBack()
                AuthEffect.OpenEmailApp -> {
                    try {
                        val intent = Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_APP_EMAIL)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        snackbarHostState.showEmmSnackbar(
                            message = "No encontramos una app de correo en tu teléfono.",
                            tone = EmmSnackbarTone.Error,
                        )
                    }
                }
                is AuthEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                    message = effect.error.toUserMessage(),
                    tone = EmmSnackbarTone.Error,
                )
                is AuthEffect.Notify -> snackbarHostState.showEmmSnackbar(
                    message = effect.message.toText(),
                    tone = when (effect.message) {
                        AuthMessage.GoogleAccountUnavailable,
                        AuthMessage.GoogleSignInFailed,
                        -> EmmSnackbarTone.Error
                        AuthMessage.ConfirmationLinkResent -> EmmSnackbarTone.Success
                    },
                )
            }
        }
    }

    AuthContent(
        state = state,
        onIntent = vm::onIntent,
    )
}

private fun AuthMessage.toText(): String = when (this) {
    AuthMessage.GoogleAccountUnavailable -> "No encontramos una cuenta de Google en este teléfono."
    AuthMessage.GoogleSignInFailed -> "No se pudo iniciar sesión con Google."
    AuthMessage.ConfirmationLinkResent -> "Listo, te reenviamos el enlace."
}

@Composable
private fun AuthContent(state: AuthUiState, onIntent: (AuthIntent) -> Unit) {
    val colors = LocalEmmColors.current

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

        when (state) {
            is AuthUiState.Form -> AuthFormStep(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
            is AuthUiState.CheckEmail -> CheckEmailStep(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AuthFormStep(
    state: AuthUiState.Form,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    val headingText = if (state.mode == AuthMode.SignIn) "Inicia sesión" else "Crea tu cuenta"
    val submitLabel = if (state.mode == AuthMode.SignIn) {
        if (state.submitting == Submitting.Email) "Entrando…" else "Iniciar sesión"
    } else {
        if (state.submitting == Submitting.Email) "Creando…" else "Crear cuenta"
    }
    val toggleLabel = if (state.mode == AuthMode.SignIn) {
        "¿No tienes cuenta? Créala"
    } else {
        "¿Ya tienes cuenta? Inicia sesión"
    }

    // Password visibility: plain remember — masked-by-default after config change is safer
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
        ) {
            Spacer(Modifier.height(spacing.s6))

            Text(
                text = headingText,
                style = type.headlineL,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(spacing.s2))
            Text(
                text = "Tu data ya vive en tu celu. Una cuenta solo la sincroniza entre tus dispositivos.",
                style = type.bodyM,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(spacing.s6))

            OutlinedCta(
                label = "Continuar con Google",
                interaction = state.submitting.toCtaInteraction(busyWhen = Submitting.Google),
                onClick = { onIntent(AuthIntent.GoogleSignInClicked) },
            )

            Spacer(Modifier.height(spacing.s4))

            // Divider with label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Hairline(modifier = Modifier.weight(1f))
                Spacer(Modifier.width(spacing.s3))
                Text(text = "o", style = type.bodyM, color = colors.textTertiary)
                Spacer(Modifier.width(spacing.s3))
                Hairline(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(spacing.s4))

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
                placeholder = "Mínimo 8 caracteres",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
            )

            Spacer(Modifier.height(spacing.s8))

            Text(
                text = toggleLabel,
                style = type.bodyM,
                color = colors.accent,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(role = Role.Button) { onIntent(AuthIntent.ToggleMode) }
                    .padding(vertical = spacing.s2),
            )

            Spacer(Modifier.height(spacing.s4))
        }

        StickyCTA(
            label = submitLabel,
            tone = CtaTone.Accent,
            interaction = state.submitting.toCtaInteraction(busyWhen = Submitting.Email),
            onClick = { onIntent(AuthIntent.Submit) },
        )
    }
}

/** Each Form CTA spins only for its own submit path and is disabled while the other runs. */
private fun Submitting.toCtaInteraction(busyWhen: Submitting): CtaInteraction = when (this) {
    Submitting.None -> CtaInteraction.Enabled
    busyWhen -> CtaInteraction.Loading
    else -> CtaInteraction.Disabled
}

@Composable
private fun CheckEmailStep(
    state: AuthUiState.CheckEmail,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.s12))

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(radii.rM)
                .background(colors.accentMuted),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MailOutline,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.height(spacing.s5))

        Text(
            text = "Revisa tu correo",
            style = type.headlineM,
            color = colors.textPrimary,
        )

        Spacer(Modifier.height(spacing.s3))

        Text(
            text = "Te mandamos un enlace a",
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Text(
            text = state.email,
            style = type.amountS.copy(fontWeight = FontWeight.W600),
            color = colors.textPrimary,
        )
        Text(
            text = "Ábrelo para confirmar tu cuenta y vuelve acá.",
            style = type.bodyM,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(spacing.s6))

        FilledCta(
            label = "Abrir mi correo",
            onClick = { onIntent(AuthIntent.OpenEmailApp) },
        )

        Spacer(Modifier.height(spacing.s3))

        OutlinedCta(
            label = "Volver a iniciar sesión",
            onClick = { onIntent(AuthIntent.BackToSignIn) },
        )

        Spacer(Modifier.height(spacing.s5))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "¿No te llegó? ",
                style = type.bodyM,
                color = colors.textTertiary,
            )
            val resendActive = !state.isResending && state.canResend
            Text(
                text = if (state.isResending) "Reenviando…" else "Reenviar enlace",
                style = type.bodyM,
                color = if (resendActive) colors.accent else colors.textTertiary,
                // Padding lives outside the conditional so the row height is identical in
                // both states — no layout jump when the link disables mid-cooldown.
                modifier = if (resendActive) {
                    Modifier.clickable(role = Role.Button) { onIntent(AuthIntent.ResendEmail) }
                } else {
                    Modifier
                }.padding(vertical = spacing.s3),
            )
        }

        Spacer(Modifier.height(spacing.s4))
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
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
) {
    val colors = LocalEmmColors.current

    var focused by remember { mutableStateOf(false) }

    val labelColor = if (focused) colors.accent else colors.textTertiary
    val underlineColor = if (focused) colors.borderFocus else colors.border

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = labelColor,
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
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .drawBehind {
                    drawLine(
                        color = underlineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(vertical = 8.dp),
            decorationBox = { inner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
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
                    if (isPassword && onTogglePasswordVisibility != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClick = onTogglePasswordVisibility,
                                ),
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Outlined.Visibility
                                } else {
                                    Icons.Outlined.VisibilityOff
                                },
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
        )
    }
}

// --- Previews ---

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun AuthFormSignInPreview() {
    EmmTheme {
        AuthContent(
            state = AuthUiState.Form(
                email = "hola@ejemplo.com",
                password = "password123",
                mode = AuthMode.SignIn,
                submitting = Submitting.None,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun AuthFormSignUpPreview() {
    EmmTheme {
        AuthContent(
            state = AuthUiState.Form(
                email = "",
                password = "",
                mode = AuthMode.SignUp,
                submitting = Submitting.None,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun AuthFormSignInLoadingPreview() {
    EmmTheme {
        AuthContent(
            state = AuthUiState.Form(
                email = "hola@ejemplo.com",
                password = "password123",
                mode = AuthMode.SignIn,
                submitting = Submitting.Email,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun AuthCheckEmailPreview() {
    EmmTheme {
        AuthContent(
            state = AuthUiState.CheckEmail(
                email = "hola@ejemplo.com",
                isResending = false,
            ),
            onIntent = {},
        )
    }
}
