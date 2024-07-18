package com.emm.justchill.hh.presentation.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@Composable
fun Transaction(
    navController: NavController,
    vm: TransactionViewModel = koinViewModel(),
) {

    val categories: List<Categories> by vm.categories.collectAsState()

    Transaction(
        categories = categories,
        isEnabledButton = vm.isEnabled,
        mountValue = vm.amount,
        onMountChange = vm::updateMount,
        descriptionValue = vm.description,
        onDescriptionChange = vm::updateDescription,
        dateValue = vm.date,
        addTransaction = vm::addTransaction,
        onCategoryChange = vm::updateCategory,
        updateDate = vm::updateCurrentDate,
        navigateUp = { navController.popBackStack() },
        navigateToCreateCategory = { navController.navigate(Category) },
        initialTransactionType = vm.transactionType,
        onOptionSelected = vm::updateTransactionType,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Transaction(
    categories: List<Categories> = emptyList(),
    isEnabledButton: Boolean = false,
    mountValue: String = "",
    onMountChange: (String) -> Unit = {},
    descriptionValue: String = "",
    onDescriptionChange: (String) -> Unit = {},
    dateValue: String = "",
    onCategoryChange: (Categories) -> Unit = {},
    addTransaction: () -> Unit = {},
    updateDate: (Long?) -> Unit = {},
    navigateUp: () -> Unit = {},
    navigateToCreateCategory: () -> Unit = {},
    initialTransactionType: TransactionType = TransactionType.INCOME,
    onOptionSelected: (TransactionType) -> Unit = {},
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
                title = { Text(text = "Agregar gasto", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) },
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
                    FilledTonalButton(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        onClick = { setShowDialog(true) }
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Filled.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(5.dp))
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

            val (text, setText) = remember {
                mutableStateOf("")
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
                    addTransaction()
                    navigateUp()
                },
                shape = RoundedCornerShape(10.dp),
                enabled = isEnabledButton,
            ) {
                Text(text = "GUARDAR")
            }
        }
    }
}

@Composable
fun Date(
    dateValue: String,
    showDatePicker: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Fecha")
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDatePicker()
                },
            value = dateValue,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.Black,
                disabledPlaceholderColor = Color.Black
            ),
            placeholder = {
                Text(text = "Fecha")
            }
        )
    }
}

@Composable
fun Mount(mountValue: String, onMountChange: (String) -> Unit) {

    Column(Modifier.fillMaxWidth()) {

        Text(text = "Cantidad")

        Spacer(modifier = Modifier.height(5.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = mountValue,
            onValueChange = onMountChange,
            placeholder = {
                Text(text = "Cantidad")
            },
            prefix = {
                Text(text = "S/ ")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDialog(
    categories: List<Categories> = emptyList(),
    navigateToCreateCategory: () -> Unit = {},
    onDismissRequest: () -> Unit,
) {

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .heightIn(max = screenHeight * 0.8f)
                .clip(RoundedCornerShape(50f))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = Color.Black,
                    shape = RoundedCornerShape(50f)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (categories.isEmpty()) {
                    Text(text = "No tienes categorias creadas")
                } else {
                    FlowRow(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        categories.forEachIndexed { _, it ->
                            SuggestionChip(
                                enabled = true,
                                onClick = { },
                                label = { Text(text = it.name) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color.Green
                                )
                            )
                        }
                    }
                }

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    onClick = navigateToCreateCategory,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.Green
                    )
                ) {
                    Text(text = "Crear")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IncomePreview() {
    EmmTheme {
        Transaction()
    }
}