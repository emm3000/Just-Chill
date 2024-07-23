package com.emm.justchill.hh.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.presentation.Login
import com.emm.justchill.hh.presentation.Main
import org.koin.androidx.compose.koinViewModel

@Composable
fun Login(
    navController: NavController,
    vm: LoginViewModel = koinViewModel()
) {

    val state by vm.loginState.collectAsState()

    Login(
        state = state,
        email = vm.email,
        updateEmail = vm::updateEmail,
        password = vm.password,
        updatePassword = vm::updatePassword,
        onLogin = vm::login,
        onRegister = vm::register,
        navigateToHome = {
            navController.navigate(Main) {
                popUpTo<Login> {
                    inclusive = true
                }
            }
        }
    )
}

@Composable
private fun Login(
    state: LoginUi = LoginUi(),
    email: String = "",
    updateEmail: (String) -> Unit = {},
    password: String = "",
    updatePassword: (String) -> Unit = {},
    onLogin: () -> Unit = {},
    onRegister: () -> Unit = {},
    navigateToHome: () -> Unit = {},
) {

    LaunchedEffect(state) {
        if (state.successLogin) {
            navigateToHome()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val focusRequester = remember { FocusRequester() }
            Email(
                email = email,
                updateEmail = updateEmail,
                onNext = {
                    focusRequester.requestFocus()
                }
            )
            Password(
                modifier = Modifier
                    .focusRequester(focusRequester),
                password = password,
                updatePassword = updatePassword
            )
            val focusManager = LocalFocusManager.current
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLogin()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryButtonColor
                ),
                shape = RoundedCornerShape(25)
            ) {
                Text(
                    text = "Entrar",
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    onRegister()
                },
                modifier = Modifier.fillMaxWidth()
                    .height(45.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextColor
                ),
                shape = RoundedCornerShape(25),
                border = BorderStroke(1.dp, TextColor)
            ) {
                Text(
                    text = "Registrar y entrar",
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            if (state.errorMsg != null) {
                Text(text = state.errorMsg, color = Color.Red)
            }
        }
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Gray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun Password(
    modifier: Modifier = Modifier,
    password: String,
    updatePassword: (String) -> Unit
) {

    val (showPassword, setShowPassword) = remember {
        mutableStateOf(true)
    }

    val focusManager = LocalFocusManager.current

    TextField(
        value = password,
        onValueChange = updatePassword,
        modifier = modifier.fillMaxWidth(),
        label = {
            LabelTextField(text = "Password")
        },
        placeholder = {
            PlaceholderTextField(text = "Ingrese su password")
        },
        colors = textFieldColors(),
        textStyle = textFieldTextStyle(),
        visualTransformation = if (showPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = {
            val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
            IconButton(onClick = { setShowPassword(!showPassword) }) {
                Icon(imageVector = icon, contentDescription = null)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        )
    )
}

@Composable
private fun Email(
    email: String,
    updateEmail: (String) -> Unit,
    onNext: () -> Unit,
) {

    TextField(
        value = email,
        onValueChange = updateEmail,
        modifier = Modifier.fillMaxWidth(),
        label = { LabelTextField("Email") },
        placeholder = { PlaceholderTextField("Ingrese su email") },
        textStyle = textFieldTextStyle(),
        colors = textFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() }
        )
    )

}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = TextColor
)

private fun textFieldTextStyle() = TextStyle(
    color = TextColor,
    fontFamily = LatoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp
)

@Composable
fun PlaceholderTextField(text: String) {
    Text(
        text = text,
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Normal,
        color = PlaceholderOrLabel
    )
}

@Composable
fun LabelTextField(text: String) {
    Text(
        text = text,
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Normal,
        color = PlaceholderOrLabel,
        fontSize = 16.sp
    )
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    EmmTheme {
        Login(
            email = "random@gmail.com"
        )
    }
}