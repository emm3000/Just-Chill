package com.emm.justchill.hh.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.emm.justchill.hh.shared.EmmTextInput
import com.emm.justchill.hh.transaction.components.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.components.NewButton
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
            EmmCenteredToolbar(
                title = "Agregar Cuenta",
                navigationIconClick = Icons.Default.Close,
                onNavigationIconClick = {
                },
            )
        },
        bottomBar = {
            NewButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                enabled = state.isEnabled,
                onClick = {
                    onAction(AddAccountAction.OnSave)
                    navigateToBack()
                },
                title = "Crear Cuenta",
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            EmmTextInput(
                value = state.name,
                onChange = { onAction(AddAccountAction.OnNameChange(it)) },
                label = "Nombre del a cuenta *",
                placeholder = "ejm. Gasto diario",
                modifier = Modifier,
            )

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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddAccountScreenPreview() {
    EmmTheme {
        AddAccountScreen(
            state = AddAccountUiState(),
            onAction = {},
        )
    }
}