package com.emm.justchill.hh.account.presentation

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
import androidx.compose.ui.text.input.TextFieldValue
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
    amount: TextFieldValue = TextFieldValue(),
    updateAmount: (TextFieldValue) -> Unit = {},
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

            Text(
                text = "Monto inicial",
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )

            EmmAmountChill(
                value = amount,
                onValueChange = updateAmount,
                modifier = Modifier.fillMaxWidth()
            )

            EmmTextFieldChill(
                value = name,
                onChange = updateName,
                label = "Nombre *",
                placeholder = "Ingresa el nombre",
                modifier = Modifier
            )

            EmmTextFieldChill(
                value = description,
                onChange = updateDescription,
                label = "Descripción (opcional)",
                placeholder = "Ingresa la descripción",
                modifier = Modifier
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