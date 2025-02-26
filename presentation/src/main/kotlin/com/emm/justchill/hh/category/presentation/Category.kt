package com.emm.justchill.hh.category.presentation

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
import com.emm.justchill.hh.shared.shared.EmmTransactionRadioButton
import com.emm.justchill.hh.transaction.presentation.EmmToolbarTitle
import com.emm.justchill.hh.transaction.presentation.TransactionType
import org.koin.androidx.compose.koinViewModel

@Composable
fun Category(
    navController: NavController,
    vm: CategoryViewModel = koinViewModel(),
) {

    Category(
        name = vm.name,
        updateName = vm::updateName,
        transactionType = vm.transactionType,
        updateTransactionType = vm::updateTransactionType,
        description = vm.name,
        updateDescription = vm::updateDescription,
        saveAction = vm::save,
        isEnabledButton = vm.isEnabled,
        navigateToBack = {
            navController.popBackStack()
        }
    )
}

@Composable
private fun Category(
    name: String = "",
    updateName: (String) -> Unit = {},
    transactionType: TransactionType = TransactionType.INCOME,
    updateTransactionType: (TransactionType) -> Unit = {},
    description: String = "",
    updateDescription: (String) -> Unit = {},
    saveAction: () -> Unit = {},
    isEnabledButton: Boolean = true,
    navigateToBack: () -> Unit = {},
) {

    Scaffold(
        modifier = Modifier,
        topBar = {
            EmmToolbarTitle(
                title = "Agregar categoría",
                navigationIconClick = {
                    navigateToBack()
                },
                modifier = Modifier.fillMaxWidth()
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

            EmmTextInput(
                modifier = Modifier,
                label = "Nombre *",
                placeholder = "Ingresa el nombre",
                value = name,
                onChange = updateName
            )

            EmmTransactionRadioButton(
                modifier = Modifier
                    .fillMaxWidth(),
                selectedOption = transactionType,
                onOptionSelected = updateTransactionType
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
                    saveAction()
                    navigateToBack()
                },
                enabled = isEnabledButton,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

}

@PreviewLightDark
@Composable
fun CategoryPreview() {

    EmmTheme {
        Category()
    }
}