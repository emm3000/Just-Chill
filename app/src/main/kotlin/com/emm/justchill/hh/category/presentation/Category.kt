package com.emm.justchill.hh.category.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.PrimaryDisableButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.core.theme.TextDisableColor
import com.emm.justchill.hh.shared.shared.TextFieldWithLabel
import com.emm.justchill.hh.shared.shared.TransactionRadioButton
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

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                modifier = Modifier,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                ),
                title = {
                    Text(
                        text = "Agregar categoría",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = LatoFontFamily,
                        color = TextColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed {
                        navigateToBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = TextColor
                        )
                    }
                },
            )
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(horizontal = 20.dp)
                .padding(vertical = 10.dp)
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            TextFieldWithLabel(
                modifier = Modifier,
                label = "Nombre *",
                placeholder = "Ingresa el nombre",
                value = name,
                onChange = updateName
            )

            TransactionRadioButton(
                modifier = Modifier
                    .fillMaxWidth(),
                selectedOption = transactionType,
                onOptionSelected = updateTransactionType
            )

            TextFieldWithLabel(
                modifier = Modifier,
                label = "Descripción (opcional)",
                placeholder = "Ingresa la descripción",
                value = description,
                onChange = updateDescription
            )

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onClick = dropUnlessResumed {
                    saveAction()
                    navigateToBack()
                },
                enabled = isEnabledButton,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = PrimaryButtonColor,
                    disabledContainerColor = PrimaryDisableButtonColor,
                    contentColor = TextColor,
                    disabledContentColor = TextDisableColor
                ),
                shape = RoundedCornerShape(25)
            ) {
                Text(
                    text = "Guardar",
                    fontFamily = LatoFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun CategoryPreview(modifier: Modifier = Modifier) {

    EmmTheme {
        Category()
    }
}