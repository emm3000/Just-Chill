package com.emm.justchill.hh.presentation.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.emm.justchill.Categories
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.presentation.auth.LabelTextField
import com.emm.justchill.hh.presentation.transaction.TransactionLabel

@Composable
fun DropDownContainer(
    categories: List<Categories>,
    onCategoryChange: (Categories) -> Unit,
    text: String,
    setText: (String) -> Unit,
) {

    if (categories.isNotEmpty()) {
        Column {
            TransactionLabel(text = "Seleccionar categoría")
            Spacer(modifier = Modifier.height(5.dp))
            DropDown(
                onCategoryChange = onCategoryChange,
                categories = categories,
                text = text,
                setText = setText
            )
        }
    }

}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DropDown(
    onCategoryChange: (Categories) -> Unit,
    categories: List<Categories>,
    text: String,
    setText: (String) -> Unit,
) {

    val (isExpanded, setIsExpanded) = remember {
        mutableStateOf(false)
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
                LabelTextField("Seleccione la categoría")
            },
            textStyle = TextStyle(
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = TextColor
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextColor
            )
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