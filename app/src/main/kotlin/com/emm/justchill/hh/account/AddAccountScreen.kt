package com.emm.justchill.hh.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.shared.shared.EmmTextInput
import com.emm.justchill.hh.transaction.EmmToolbarTitle
import com.emm.justchill.hh.transaction.NewButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddAccountScreen(
    navBackStack: NavBackStack<NavKey>,
    vm: AddAccountViewModel = koinViewModel(),
) {

    AddAccountScreen(
        state = vm.state,
        onAction = vm::onAction,
        navigateToBack = navBackStack::removeLastOrNull,
    )
}

@Composable
private fun AddAccountScreen(
    state: AddAccountUiState,
    onAction: (AddAccountAction) -> Unit,
    navigateToBack: () -> Unit = {},
) {

    Scaffold(
        modifier = Modifier,
        topBar = {
            EmmToolbarTitle(
                title = "Agregar cuenta",
                navigationIconClick = navigateToBack,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Text(
                text = "Monto inicial",
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )

            EmmAmountChill(
                value = state.balance,
                onValueChange = { onAction(AddAccountAction.OnAmountChange(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            EmmTextInput(
                value = state.name,
                onChange = { onAction(AddAccountAction.OnNameChange(it)) },
                label = "Nombre *",
                placeholder = "Ingresa el nombre",
                modifier = Modifier,
            )

            NewButton(
                title = "Crear cuenta",
                onClick = {
                    onAction(AddAccountAction.OnSave)
                    navigateToBack()
                },
                enabled = state.isEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddAccountScreenPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        AddAccountScreen(
            state = AddAccountUiState(),
            onAction = {},
        )
    }
}