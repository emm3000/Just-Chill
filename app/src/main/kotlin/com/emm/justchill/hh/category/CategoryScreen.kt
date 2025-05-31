package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.shared.shared.EmmPrimaryButton
import com.emm.justchill.hh.shared.shared.EmmTextInput
import com.emm.justchill.hh.transaction.EmmCenteredToolbar
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            val screenWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp
            val current = LocalSoftwareKeyboardController.current
            EmmCenteredToolbar(
                title = "Agregar Categoría",
                modifier = Modifier.requiredWidth(screenWidthDp),
                navigationIconClick = Icons.Default.Close,
                onNavigationIconClick = {
                    current?.hide()
                    navigateToBack()
                }
            )

            EmmTextInput(
                modifier = Modifier,
                label = "Nombre",
                placeholder = "Ingresa el nombre",
                value = state.name,
                onChange = { onAction(CategoryAction.OnNameChange(it)) }
            )
        }

        EmmPrimaryButton(
            text = "Guardar",
            onClick = {
                onAction(CategoryAction.OnSave)
                navigateToBack()
            },
            enabled = state.isAllFieldValidated,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .imePadding()
                .padding(bottom = 20.dp)
        )
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