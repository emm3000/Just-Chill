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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
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
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.OutlinedCta
import com.emm.justchill.core.ui.atoms.StickyCTA
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    vm: AuthViewModel = koinViewModel(),
    googleClient: GoogleCredentialClient = koinInject(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val context = LocalContext.current

    BackHandler(enabled = state.step == AuthStep.CheckEmail) { vm.onIntent(AuthIntent.Back) }

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
                        snackbarHostState.showSnackbar("No encontramos una app de correo en tu teléfono.")
                    }
                }
                is AuthEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is AuthEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is AuthEffect.LaunchGoogleSignIn -> launch {
                    var delivered = false
                    try {
                        val result = googleClient.signIn(context, effect.serverClientId)
                        delivered = true
                        vm.onIntent(result.toIntent())
                    } finally {
                        // The composition can die mid-flow (e.g. configuration change while the
                        // credential sheet is open). Without this fallback the ViewModel would
                        // stay isLoading forever, leaving the screen permanently disabled.
                        if (!delivered) vm.onIntent(AuthIntent.GoogleSignInCancelled)
                    }
                }
            }
        }
    }

    AuthContent(
        state = state,
        onIntent = vm::onIntent,
    )
}

/** Maps [GoogleCredentialClient.Result] to the appropriate [AuthIntent]. */
private fun GoogleCredentialClient.Result.toIntent(): AuthIntent = when (this) {
    is GoogleCredentialClient.Result.Success -> AuthIntent.GoogleTokenReceived(idToken, rawNonce)
    GoogleCredentialClient.Result.Cancelled -> AuthIntent.GoogleSignInCancelled
    GoogleCredentialClient.Result.NoCredentials -> AuthIntent.GoogleSignInUnavailable
    is GoogleCredentialClient.Result.Failure -> AuthIntent.GoogleSignInErrored(cause)
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

        when (state.step) {
            AuthStep.Form -> AuthFormStep(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
            AuthStep.CheckEmail -> CheckEmailStep(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AuthFormStep(
    state: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    val headingText = if (state.mode == AuthMode.SignIn) "Inicia sesión" else "Crea tu cuenta"
    val submitLabel = if (state.mode == AuthMode.SignIn) {
        if (state.isLoading) "Entrando…" else "Iniciar sesión"
    } else {
        if (state.isLoading) "Creando…" else "Crear cuenta"
    }
    val toggleLabel = if (state.mode == AuthMode.SignIn) {
        "¿No tienes cuenta? Créala"
    } else {
        "¿Ya tienes cuenta? Inicia sesión"
    }

    // Password visibility is a pure view concern — not in state
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
        ) {
            Spacer(Modifier.height(spacing.s6))

            // Heading block
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
                enabled = !state.isLoading,
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
                    .clickable { onIntent(AuthIntent.ToggleMode) }
                    .padding(vertical = spacing.s2),
            )

            Spacer(Modifier.height(spacing.s4))
        }

        StickyCTA(
            label = submitLabel,
            tone = CtaTone.Accent,
            enabled = !state.isLoading,
            loading = state.isLoading,
            onClick = { onIntent(AuthIntent.Submit) },
        )
    }
}

@Composable
private fun CheckEmailStep(
    state: AuthUiState,
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

        // 56dp icon tile
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
            text = state.confirmationEmail,
            style = type.amountS.copy(fontWeight = FontWeight.W600),
            color = colors.textPrimary,
        )
        Text(
            text = "Ábrelo para confirmar tu cuenta y vuelve acá.",
            style = type.bodyM,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(spacing.s6))

        // Primary "Abrir mi correo" button — inline private composable
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

        // Resend footer
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "¿No te llegó? ",
                style = type.bodyM,
                color = colors.textTertiary,
            )
            if (state.isResending) {
                Text(
                    text = "Reenviando…",
                    style = type.bodyM,
                    color = colors.textTertiary,
                )
            } else {
                Text(
                    text = "Reenviar enlace",
                    style = type.bodyM,
                    color = colors.accent,
                    modifier = Modifier.clickable { onIntent(AuthIntent.ResendEmail) },
                )
            }
        }

        Spacer(Modifier.height(spacing.s4))
    }
}

/** Full-width filled primary button for the check-email step. */
@Composable
private fun FilledCta(label: String, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(radii.rL)
            .background(colors.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = type.titleM,
            color = colors.textOnAccent,
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
                        strokeWidth = 1f,
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
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.Visibility
                            } else {
                                Icons.Outlined.VisibilityOff
                            },
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = colors.textTertiary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(onClick = onTogglePasswordVisibility),
                        )
                    }
                }
            },
        )
    }
}
