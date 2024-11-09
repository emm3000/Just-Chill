package com.emm.justchill.hh.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.shared.shared.EmmPrimaryButton
import com.emm.justchill.hh.shared.shared.EmmTextInput
import com.emm.justchill.hh.transaction.presentation.EmmTextAmount
import com.emm.justchill.hh.transaction.presentation.EmmToolbarTitle
import org.koin.androidx.compose.koinViewModel

@Composable
fun Account(
    navController: NavController,
    vm: AccountViewModel = koinViewModel(),
) {

    Account(
        amount = vm.amount,
        updateAmount = vm::updateAmount,
        name = vm.name,
        updateName = vm::updateName,
        description = vm.description,
        updateDescription = vm::updateDescription,
        navigateToBack = {
            navController.popBackStack()
        },
        isEnabledButton = vm.isEnabled,
        save = vm::save
    )
}

@Composable
fun Account(
    amount: String = "",
    updateAmount: (String) -> Unit = {},
    name: String = "",
    updateName: (String) -> Unit = {},
    description: String = "",
    updateDescription: (String) -> Unit = {},
    navigateToBack: () -> Unit = {},
    isEnabledButton: Boolean = true,
    save: () -> Unit = {},
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .padding(vertical = 10.dp)
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            EmmTextAmount(
                value = amount,
                onAmountChange = updateAmount,
                label = "Monto",
                placeholder = "Ingrese un monto",
                modifier = Modifier.fillMaxWidth()
            )

            EmmTextInput(
                modifier = Modifier,
                label = "Nombre *",
                placeholder = "Ingresa el nombre",
                value = name,
                onChange = updateName
            )

            EmmTextInput(
                modifier = Modifier,
                label = "Descripción (opcional)",
                placeholder = "Ingresa la descripción",
                value = description,
                onChange = updateDescription
            )

            EmmPrimaryButton(
                text = "Guardar",
                onClick = {
                    save()
                    navigateToBack()
                },
                enabled = isEnabledButton,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@PreviewLightDark
@Composable
fun AccountPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Account()
    }
}