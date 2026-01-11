package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.shared.EmmTextInput
import com.emm.justchill.hh.transaction.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.NewButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun CategoryScreen(
    navController: NavBackStack<NavKey>,
    vm: CategoryViewModel = koinViewModel(),
) {

    CategoryScreen(
        state = vm.categoryUiState,
        onAction = vm::onAction,
        navigateToBack = navController::removeLastOrNull
    )
}

@Composable
private fun CategoryScreen(
    state: CategoryUiState,
    onAction: (CategoryAction) -> Unit,
    navigateToBack: () -> Unit = {},
) {

    val current = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            EmmCenteredToolbar(
                title = "Agregar Categoría",
                navigationIconClick = Icons.Default.Close,
                onNavigationIconClick = {
                    current?.hide()
                    navigateToBack()
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Spacer(Modifier.height(15.dp))

            EmmTextInput(
                modifier = Modifier,
                label = "Nombre",
                placeholder = "Ingresa el nombre",
                value = state.name,
                onChange = { onAction(CategoryAction.OnNameChange(it)) }
            )

            NewButton(
                title = "Guardar",
                onClick = {
                    current?.hide()
                    onAction(CategoryAction.OnSave)
                    navigateToBack()
                },
                enabled = state.isAllFieldValidated,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryPreview() {

    EmmTheme {
        CategoryScreen(
            state = CategoryUiState(),
            onAction = {},
        )
    }
}