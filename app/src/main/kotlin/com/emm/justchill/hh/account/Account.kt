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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.shared.shared.EmmPrimaryButton
import com.emm.justchill.hh.shared.shared.EmmTextFieldChill
import com.emm.justchill.hh.transaction.presentation.EmmToolbarTitle
import org.koin.androidx.compose.koinViewModel

@Composable
fun Account(
    navController: NavController,
    vm: AccountViewModel = koinViewModel(),
) {

    Account(
        state = vm.state,
        onAction = vm::onAction,
        navigateToBack = {
            navController.popBackStack()
        },
    )
}

@Composable
fun Account(
    state: AccountUiState,
    onAction: (AccountAction) -> Unit,
    navigateToBack: () -> Unit = {},
) {

    Scaffold(
        modifier = Modifier,
        topBar = {
            EmmToolbarTitle(
                title = "Agregar cuenta",
                navigationIconClick = navigateToBack,
                modifier = Modifier,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .padding(vertical = 10.dp)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
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
                value = state.amount,
                onValueChange = { onAction(AccountAction.OnAmountChange(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            EmmTextFieldChill(
                value = state.name,
                onChange = { onAction(AccountAction.OnNameChange(it)) },
                label = "Nombre *",
                placeholder = "Ingresa el nombre",
                modifier = Modifier
            )

            EmmTextFieldChill(
                value = state.description,
                onChange = { onAction(AccountAction.OnDescriptionChange(it)) },
                label = "Descripción (opcional)",
                placeholder = "Ingresa la descripción",
                modifier = Modifier,
            )

            EmmPrimaryButton(
                text = "Guardar",
                onClick = {
                    onAction(AccountAction.OnSave)
                    navigateToBack()
                },
                enabled = state.isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
fun AccountPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Account(
            state = AccountUiState(),
            onAction = {},
        )
    }
}