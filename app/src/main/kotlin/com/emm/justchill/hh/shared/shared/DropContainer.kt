package com.emm.justchill.hh.shared.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.auth.presentation.LabelTextField
import com.emm.justchill.hh.home.EmmHeading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EmmDropDown(
    textLabel: String,
    textPlaceholder: String,
    items: List<T>,
    itemSelected: T?,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
    ) {

        val (isExpanded, setIsExpanded) = remember {
            mutableStateOf(false)
        }

        val selected: String by remember(itemSelected) {
            mutableStateOf(itemSelected?.toString() ?: "")
        }

        EmmHeading(textLabel)
        Spacer(modifier = Modifier.height(5.dp))
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = setIsExpanded,
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {  },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                placeholder = {
                    LabelTextField(textPlaceholder)
                },
                textStyle = TextStyle(
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground
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
                modifier = Modifier
                    .exposedDropdownSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        modifier = Modifier,
                        text = {
                            Text(
                                text = item.toString(),
                                fontFamily = LatoFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        onClick = {
                            onItemSelected(item)
                            setIsExpanded(false)
                        }
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DropDownContainerPreview() {
    EmmTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            EmmDropDown(
                textLabel = "Accounts",
                textPlaceholder = "Pick an account",
                items = listOf(
                    ("random"),
                    ("random"),
                    ("random"),
                    ("random"),
                    ("random"),
                ),
                onItemSelected = {},
                itemSelected = "random322",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}