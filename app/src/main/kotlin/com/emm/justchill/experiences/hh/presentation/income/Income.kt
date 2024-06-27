package com.emm.justchill.experiences.hh.presentation.income

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.emm.justchill.Categories
import com.emm.justchill.core.Result
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.experiences.hh.presentation.Category
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
        navigateUp = { navController.navigateUp() },
        navigateToCreateCategory = { navController.navigate(Category) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    navigateToCreateCategory: () -> Unit = {},
) {

    val (showDialog, setShowDialog) = rememberSaveable {
        mutableStateOf(false)
    }

    if (showDialog) {
        TaskDialog(
            categories = (categories as? Result.Success)?.data.orEmpty(),
            onDismissRequest = { setShowDialog(false) },
            navigateToCreateCategory = {
                setShowDialog(false)
                navigateToCreateCategory()
            }
        )
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = { Text(text = "Agregar gasto", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    OutlinedButton(onClick = { setShowDialog(true) }) {
                        Text(text = "CATEGORÍA")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

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

            Spacer(modifier = Modifier.height(15.dp))
            TextFieldWithLabel(
                label = "Fecha",
                value = dateValue,
                onChange = onDateChange
            )
            Spacer(modifier = Modifier.height(15.dp))
            if (categories is Result.Success) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Seleccionar categoría"
                )
                Spacer(modifier = Modifier.height(5.dp))
                DropDown(
                    onCategoryChange = onCategoryChange,
                    categories = categories.data
                )
            }

            Spacer(modifier = Modifier.height(15.dp))
            TextFieldWithLabel(
                modifier = Modifier
                    .height(140.dp),
                label = "En que gaste",
                value = descriptionValue,
                onChange = onDescriptionChange
            )
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
        }
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
                        categories.forEachIndexed { index, it ->
                            FilterChip(
                                selected = true,
                                onClick = { },
                                label = { Text(text = it.name) },
                            )
                        }
                    }
                }

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    onClick = navigateToCreateCategory,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(text = "GUARDAR")
                }
            }
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