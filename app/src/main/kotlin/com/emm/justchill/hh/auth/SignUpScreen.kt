package com.emm.justchill.hh.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun SignUpScreen(
    state: SignUpUiState = SignUpUiState(),
    onAction: (SignUpAction) -> Unit = {},
    onBack: () -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        Header()

        EmailField(state, onAction)

        FirstPassword(state, onAction)

        SecondPassword(state, onAction)

        CheckTermsAndConditions(state, onAction)

        SignUpButton(onAction, state)

        ErrorMessage(state)

        Footer(onBack)

        Spacer(modifier = Modifier.weight(0.5f))
    }
}

@Composable
private fun Footer(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("¿Ya tienes una cuenta?", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        TextButton(onClick = { onBack() }) {
            Text("Inicia Sesión")
        }
    }
}

@Composable
private fun ErrorMessage(state: SignUpUiState) {
    state.error?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SignUpButton(onAction: (SignUpAction) -> Unit, state: SignUpUiState) {
    val current = LocalSoftwareKeyboardController.current

    Button(
        onClick = {
            current?.hide()
            onAction(SignUpAction.SignUp)
        },
        enabled = state.isValidFields,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        AnimatedVisibility(
            visible = state.isLoading.not(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "Registrarse",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun CheckTermsAndConditions(state: SignUpUiState, onAction: (SignUpAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(
            checked = state.isChecked,
            onCheckedChange = { onAction(SignUpAction.OnCheckedChange(it)) },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = if (state.isChecked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        val annotatedString = buildAnnotatedString {
            append("Acepto los ")
            pushStringAnnotation(tag = "terms", annotation = "https://your-app.com/terms")
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append("Términos y Condiciones")
            }
            pop()
        }
        ClickableText(
            text = annotatedString,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset)
                    .firstOrNull()?.let {
                        println("Click en términos: ${it.item}")
                    }
            },
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@Composable
private fun SecondPassword(state: SignUpUiState, onAction: (SignUpAction) -> Unit) {
    val (showSecondPassword, setShowSecondPassword) = remember { mutableStateOf(false) }
    OutlinedTextField(
        value = state.confirmPassword,
        onValueChange = { onAction(SignUpAction.OnConfirmPasswordChange(it)) },
        label = {
            Text(
                text = "Confirmar Contraseña",
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (showSecondPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (showSecondPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (showSecondPassword) "Ocultar contraseña" else "Mostrar contraseña"
            IconButton(onClick = { setShowSecondPassword(!showSecondPassword) }) {
                Icon(imageVector = image, contentDescription = description)
            }
        },
        isError = false,
        supportingText = {
            if (state.passwordError != null) {
                Text(text = state.passwordError)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
        )
    )
}

@Composable
private fun FirstPassword(state: SignUpUiState, onAction: (SignUpAction) -> Unit) {
    val (showFirstPassword, setShowFirstPassword) = remember { mutableStateOf(false) }
    OutlinedTextField(
        value = state.password,
        onValueChange = { onAction(SignUpAction.OnPasswordChange(it)) },
        label = {
            Text(
                text = "Contraseña",
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (showFirstPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (showFirstPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (showFirstPassword) "Ocultar contraseña" else "Mostrar contraseña"
            IconButton(onClick = { setShowFirstPassword(!showFirstPassword) }) {
                Icon(imageVector = image, contentDescription = description)
            }
        },
        isError = false,
        supportingText = {
            if (state.passwordError != null) {
                Text(text = state.passwordError)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
        )
    )
}

@Composable
private fun EmailField(state: SignUpUiState, onAction: (SignUpAction) -> Unit) {
    OutlinedTextField(
        value = state.email,
        onValueChange = { onAction(SignUpAction.OnEmailChange(it)) },
        label = {
            Text(
                text = "Correo electronico",
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = false,
        supportingText = {
            if (state.emailError != null) {
                Text(text = state.emailError)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
        )
    )
}

@Composable
private fun Header() {
    Text(
        text = "Crea tu cuenta",
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Únete a nuestra comunidad",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    EmmTheme {
        SignUpScreen(
            state = SignUpUiState(
                email = "luke.trevino@example.com",
                emailError = null,
                password = "commune",
                passwordError = null,
                confirmPassword = "sadipscing",
                isValidFields = false,
                isChecked = false,
                success = false,
                isLoading = false,
                error = null
            ), onAction = {}, onBack = {}

        )
    }
}