package com.emm.justchill.hh.shared.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.auth.presentation.LabelTextField
import com.emm.justchill.hh.home.EmmHeading
import com.emm.justchill.hh.transaction.presentation.TransactionLabel

@Composable
fun DropDownContainer(
    account: List<Account>,
    onAccountChange: (Account) -> Unit,
    text: String,
    setText: (String) -> Unit,
) {

    Column {
        TransactionLabel(text = "Seleccionar cuenta")
        Spacer(modifier = Modifier.height(5.dp))
        DropDown(
            onAccountChange = onAccountChange,
            accounts = account,
            text = text,
            setText = setText
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDown(
    onAccountChange: (Account) -> Unit,
    accounts: List<Account>,
    text: String,
    setText: (String) -> Unit,
) {

    val (isExpanded, setIsExpanded) = remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = setIsExpanded) {
        @Suppress("DEPRECATION")
        OutlinedTextField(
            value = text,
            onValueChange = setText,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            placeholder = {
                LabelTextField("Seleccione una cuenta")
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
            modifier = Modifier
                .exposedDropdownSize()
                .background(BackgroundColor)
        ) {
            accounts.forEach {
                DropdownMenuItem(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, PlaceholderOrLabel), shape = RectangleShape),
                    text = {
                        Text(
                            text = it.nameWithBalance,
                            fontFamily = LatoFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextColor
                        )
                    },
                    onClick = {
                        onAccountChange(it)
                        setText(it.nameWithBalance)
                        setIsExpanded(false)
                    }
                )
            }
        }
    }
}

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