package com.emm.justchill.hh.category

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
import org.koin.androidx.compose.koinViewModel

@Composable
fun CategoryScreen(
    navController: NavController,
    vm: CategoryViewModel = koinViewModel(),
) {

    CategoryScreen(
        state = vm.categoryUiState,
        onAction = vm::onAction,
        navigateToBack = {
            navController.popBackStack()
        }
    )
}

@Composable
private fun CategoryScreen(
    state: CategoryUiState,
    onAction: (CategoryAction) -> Unit,
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

            EmmTextInput(
                modifier = Modifier,
                label = "Nombre *",
                placeholder = "Ingresa el nombre",
                value = state.name,
                onChange = { onAction(CategoryAction.OnNameChange(it)) }
            )

            EmmTransactionRadioButton(
                modifier = Modifier
                    .fillMaxWidth(),
                selectedOption = state.transactionType,
                onOptionSelected = { onAction(CategoryAction.OnTransactionTypeChange(it)) }
            )

            EmmTextInput(
                modifier = Modifier,
                label = "Descripción (opcional)",
                placeholder = "Ingresa la descripción",
                value = state.description,
                onChange = { onAction(CategoryAction.OnDescriptionChange(it)) }
            )

            EmmPrimaryButton(
                text = "Guardar",
                onClick = {
                    onAction(CategoryAction.OnSave)
                    navigateToBack()
                },
                enabled = state.isAllFieldValidated,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

}

@PreviewLightDark
@Composable
fun CategoryPreview() {

    EmmTheme {
        CategoryScreen(
            state = CategoryUiState(),
            onAction = {},
        )
    }
}