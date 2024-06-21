package com.emm.justchill.experiences.hh.presentation.income

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.emm.justchill.Categories
import com.emm.justchill.core.Result
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.experiences.hh.presentation.TextFieldWithLabel
import org.koin.androidx.compose.koinViewModel

@Composable
fun Income(
    navController: NavController,
    vm: IncomeViewModel = koinViewModel(),
) {

    val categories: Result<List<Categories>> by vm.categories.collectAsState()

    Income(
        categories = categories,
        isEnabledButton = vm.isEnabled,
        mountValue = vm.mount,
        onMountChange = vm::updateMount,
        descriptionValue = vm.description,
        onDescriptionChange = vm::updateDescription,
        dateValue = vm.date,
        onDateChange = vm::updateDate,
        addTransaction = vm::addTransaction,
        onCategoryChange = vm::updateCategory,
        navigateUp = { navController.navigateUp() }
    )
}

@Composable
private fun Income(
    categories: Result<List<Categories>> = Result.Success(emptyList()),
    isEnabledButton: Boolean = false,
    mountValue: String = "",
    onMountChange: (String) -> Unit = {},
    descriptionValue: String = "",
    onDescriptionChange: (String) -> Unit = {},
    dateValue: String = "",
    onDateChange: (String) -> Unit = {},
    onCategoryChange: (Categories) -> Unit = {},
    addTransaction: () -> Unit = {},
    navigateUp: () -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "INGRESAR GASTO")

        Spacer(modifier = Modifier.height(20.dp))

        TextFieldWithLabel(
            label = "Cantidad",
            value = mountValue,
            onChange = onMountChange
        )
        Spacer(modifier = Modifier.height(15.dp))
        TextFieldWithLabel(
            modifier = Modifier
                .height(140.dp),
            label = "Description",
            value = descriptionValue,
            onChange = onDescriptionChange
        )
        Spacer(modifier = Modifier.height(15.dp))
        TextFieldWithLabel(
            label = "Fecha",
            value = dateValue,
            onChange = onDateChange
        )
        Spacer(modifier = Modifier.height(15.dp))
        if (categories is Result.Success) {
            DropDown(
                onCategoryChange = onCategoryChange,
                categories = categories.data
            )
        }
        Spacer(modifier = Modifier.height(15.dp))

        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            onClick = addTransaction,
            shape = RoundedCornerShape(10.dp),
            enabled = isEnabledButton,
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
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        ) {
            Text(text = "CANCELAR")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DropDown(
    onCategoryChange: (Categories) -> Unit,
    categories: List<Categories>,
) {

    val (isExpanded, setIsExpanded) = remember {
        mutableStateOf(false)
    }

    val (text, setText) = remember {
        mutableStateOf("")
    }

    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = setIsExpanded) {
        OutlinedTextField(
            value = text,
            onValueChange = setText,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            placeholder = {
                Text(text = "Seleccione la categoría")
            }
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { setIsExpanded(false) },
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
            ),
            modifier = Modifier.exposedDropdownSize()
        ) {
            categories.forEach {
                DropdownMenuItem(
                    text = { Text(text = it.name) },
                    onClick = {
                        onCategoryChange(it)
                        setText(it.name)
                        setIsExpanded(false)
                    }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun IncomePreview() {
    EmmTheme {
        Income()
    }
}