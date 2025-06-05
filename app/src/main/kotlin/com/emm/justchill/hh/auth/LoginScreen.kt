package com.emm.justchill.hh.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    navigateToRegister: () -> Unit = {},
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Bienvenido de nuevo",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Inicia sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontFamily = LatoFontFamily,
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = {
                onAction(LoginAction.UpdateEmail(it))
            },
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
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
            )
        )

        val (showPassword, setShowPassword) = remember { mutableStateOf(false) }

        OutlinedTextField(
            value = state.password,
            onValueChange = {
                onAction(LoginAction.UpdatePassword(it))
            },
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
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                IconButton(onClick = { setShowPassword(!showPassword) }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
            )
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(onClick = { }) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    fontFamily = LatoFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = { onAction(LoginAction.Login) },
            enabled = state.isValidFields,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            AnimatedContent(
                targetState = state.isLoading,
                transitionSpec = {
                    fadeIn() + slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start) togetherWith
                            fadeOut() + slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End)
                },
                label = "ButtonContentAnimatedContent"
            ) { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 18.sp,
                        fontFamily = LatoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "¿No tienes una cuenta?",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontFamily = LatoFontFamily,
            )
            TextButton(onClick = navigateToRegister) {
                Text(
                    text = "Regístrate",
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun LabelTextField(text: String) {
    Text(
        text = text,
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 16.sp
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    EmmTheme {
        LoginScreen(
            modifier = Modifier, state = LoginUiState(
                email = "allen.waller@example.com",
                password = "mi",
                isLoading = false,
                errorMsg = null,
                isValidFields = false,
                successLogin = false
            ), onAction = {}

        )
    }
}