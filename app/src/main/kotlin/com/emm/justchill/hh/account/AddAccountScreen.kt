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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.shared.EmmTextInput
import com.emm.justchill.hh.transaction.components.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.components.NewButton
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.Composable

@Composable
fun AddAccountScreen(
    onBack: () -> Unit,
    vm: AddAccountViewModel = koinViewModel(),
) {

    AddAccountScreen(
        state = vm.state,
        onAction = vm::onAction,
        navigateToBack = onBack,
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
                label = "Nombre de la cuenta *",
                placeholder = "ejm. Gasto diario",
                modifier = Modifier,
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
