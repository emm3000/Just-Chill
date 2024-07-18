package com.emm.justchill.hh.presentation.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import com.emm.justchill.Categories
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.presentation.Category
import com.emm.justchill.hh.presentation.TextFieldWithLabel
import com.emm.justchill.hh.presentation.TransactionTypeRadioButton
import com.emm.justchill.hh.presentation.shared.DropDownContainer
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditTransaction(
    navController: NavController,
    transactionId: String,
    vm: EditTransactionViewModel = koinViewModel(parameters = { parametersOf(transactionId) })
) {

    val categories: List<Categories> by vm.categories.collectAsState()

    EditTransaction(
        categories = categories,
        isEnabledButton = vm.isEnabled,
        mountValue = vm.amount,
        onMountChange = vm::updateMount,
        descriptionValue = vm.description,
        onDescriptionChange = vm::updateDescription,
        dateValue = vm.date,
        updateTransaction = vm::updateTransaction,
        onCategoryChange = vm::updateCategory,
        updateDate = vm::updateCurrentDate,
        navigateUp = { navController.popBackStack() },
        navigateToCreateCategory = { navController.navigate(Category) },
        initialTransactionType = vm.transactionType,
        onOptionSelected = vm::updateTransactionType,
        text = vm.categoryLabel,
        setText = vm::updateCategoryLabel,
        deleteTransaction = vm::deleteTransaction
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransaction(
    categories: List<Categories> = emptyList(),
    isEnabledButton: Boolean = false,
    mountValue: String = "",
    onMountChange: (String) -> Unit = {},
    descriptionValue: String = "",
    onDescriptionChange: (String) -> Unit = {},
    dateValue: String = "",
    onCategoryChange: (Categories) -> Unit = {},
    updateTransaction: () -> Unit = {},
    updateDate: (Long?) -> Unit = {},
    navigateUp: () -> Unit = {},
    navigateToCreateCategory: () -> Unit = {},
    initialTransactionType: TransactionType = TransactionType.INCOME,
    onOptionSelected: (TransactionType) -> Unit = {},
    text: String = "",
    setText: (String) -> Unit = {},
    deleteTransaction: () -> Unit = {},
) {
    val (showDialog, setShowDialog) = rememberSaveable {
        mutableStateOf(false)
    }

    if (showDialog) {
        TaskDialog(
            categories = categories,
            onDismissRequest = { setShowDialog(false) },
            navigateToCreateCategory = {
                setShowDialog(false)
                navigateToCreateCategory()
            }
        )
    }

    val datePickerState: DatePickerState = rememberDatePickerState()

    val (showSelectDate, setShowSelectDate) = remember {
        mutableStateOf(false)
    }

    if (showSelectDate) {
        DatePickerDialog(
            onDismissRequest = {
                setShowSelectDate(false)
            },
            confirmButton = {
                OutlinedButton(onClick = {
                    updateDate(datePickerState.selectedDateMillis)
                    setShowSelectDate(false)
                }) {
                    Text(text = "Ok")
                }
            },
            dismissButton = {
                Button(onClick = { setShowSelectDate(false) }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = { Text(text = "Editar transacción", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed {
                        navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    OutlinedButton(onClick = { setShowDialog(true) }) {
                        Text(text = "Categoría")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp)
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            Mount(mountValue, onMountChange)

            TransactionTypeRadioButton(
                selectedOption = initialTransactionType,
                onOptionSelected = onOptionSelected
            )

            Date(dateValue) {
                setShowSelectDate(true)
            }

            DropDownContainer(
                categories = categories,
                onCategoryChange = onCategoryChange,
                text = text,
                setText = setText
            )

            TextFieldWithLabel(
                modifier = Modifier
                    .height(140.dp),
                label = "En que gaste",
                value = descriptionValue,
                onChange = onDescriptionChange
            )

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                onClick = dropUnlessResumed {
                    updateTransaction()
                    navigateUp()
                },
                shape = RoundedCornerShape(10.dp),
                enabled = isEnabledButton,
            ) {
                Text(text = "ACTUALIZAR", fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                onClick = dropUnlessResumed {
                    deleteTransaction()
                    navigateUp()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(text = "ELIMINAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditTransactionPreview() {
    EmmTheme {
        EditTransaction()
    }
}