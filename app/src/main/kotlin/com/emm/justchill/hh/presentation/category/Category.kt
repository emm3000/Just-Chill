package com.emm.justchill.hh.presentation.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.PrimaryDisableButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.core.theme.TextDisableColor
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.presentation.TextFieldWithLabel
import com.emm.justchill.hh.presentation.TransactionRadioButton
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
        navigateUp = { navController.popBackStack() },
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
            .background(BackgroundColor)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(
            text = "Ingresar categoria",
            fontSize = 20.sp,
            fontFamily = LatoFontFamily,
            color = TextColor,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(25.dp))

        TextFieldWithLabel(
            label = "Nombre",
            value = nameValue,
            onChange = onNameChange
        )

        Spacer(modifier = Modifier.height(25.dp))

        TransactionRadioButton(
            modifier = Modifier.fillMaxWidth(),
            selectedOption = initialTransactionType,
            onOptionSelected = onOptionSelected
        )

        Spacer(modifier = Modifier.height(20.dp))

        val keyboardController = LocalSoftwareKeyboardController.current
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            onClick = dropUnlessResumed {
                keyboardController?.hide()
                addCategory()
                navigateUp()
            },
            enabled = nameValue.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
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
        Spacer(modifier = Modifier.height(15.dp))
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            onClick = dropUnlessResumed {
                navigateUp()
            },
            shape = RoundedCornerShape(25),
            colors = ButtonDefaults.outlinedButtonColors(
                disabledContainerColor = PrimaryDisableButtonColor,
                contentColor = DeleteButtonColor,
                disabledContentColor = TextDisableColor,
            ),
            border = BorderStroke(1.dp, DeleteButtonColor)
        ) {
            Text(
                text = "Cancelar",
                fontFamily = LatoFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
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