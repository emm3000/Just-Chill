package com.emm.justchill.hh.presentation.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.presentation.TextFieldWithLabel
import com.emm.justchill.hh.presentation.TransactionTypeRadioButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun Category(
    navController: NavController,
    vm: CategoryViewModel = koinViewModel(),
) {

    Category(
        initialTransactionType = vm.transactionType,
        onOptionSelected = vm::updateTransactionType,
        addCategory = vm::addCategory,
        navigateUp = { navController.navigateUp() },
        nameValue = vm.name,
        onNameChange = vm::updateName,
    )
}

@Composable
fun Category(
    initialTransactionType: TransactionType = TransactionType.INCOME,
    onOptionSelected: (TransactionType) -> Unit = {},
    addCategory: () -> Unit = {},
    navigateUp: () -> Unit = {},
    nameValue: String = "",
    onNameChange: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(text = "Ingresar categoria")

        Spacer(modifier = Modifier.height(25.dp))

        TextFieldWithLabel(
            label = "Nombre",
            value = nameValue,
            onChange = onNameChange
        )

        Spacer(modifier = Modifier.height(25.dp))

        TransactionTypeRadioButton(
            selectedOption = initialTransactionType,
            onOptionSelected = onOptionSelected
        )

        Spacer(modifier = Modifier.height(20.dp))

        val keyboardController = LocalSoftwareKeyboardController.current
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            onClick = {
                keyboardController?.hide()
                addCategory()
                navigateUp()
            },
            shape = RoundedCornerShape(10.dp),
            enabled = nameValue.isNotEmpty(),
        ) {
            Text(text = "GUARDAR")
        }
        Spacer(modifier = Modifier.height(15.dp))
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            onClick = navigateUp,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(text = "CANCELAR")
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