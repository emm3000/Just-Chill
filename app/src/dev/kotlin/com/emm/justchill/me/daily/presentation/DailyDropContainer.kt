package com.emm.justchill.me.daily.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.auth.presentation.LabelTextField
import com.emm.justchill.hh.transaction.presentation.TransactionLabel

data class Gaa(val name: String)

@Composable
fun DailyDropContainer(
    currentItem: Gaa,
    items: List<Gaa>,
    onItemChange: (Gaa) -> Unit,
) {

    Column {
        TransactionLabel(text = "Seleccionar chofer")
        Spacer(modifier = Modifier.height(5.dp))
        DropDown(
            onItemChange = onItemChange,
            items = items,
            currentItem = currentItem,
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropDown(
    onItemChange: (Gaa) -> Unit,
    items: List<Gaa>,
    currentItem: Gaa,
) {

    val (isExpanded, setIsExpanded) = remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = setIsExpanded) {
        @Suppress("DEPRECATION")
        OutlinedTextField(
            value = currentItem.name,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            placeholder = {
                LabelTextField("Chofer")
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
            items.forEach {
                DropdownMenuItem(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, PlaceholderOrLabel), shape = RectangleShape),
                    text = {
                        Text(
                            text = it.name,
                            fontFamily = LatoFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextColor
                        )
                    },
                    onClick = {
                        onItemChange(it)
                        setIsExpanded(false)
                    }
                )
            }
        }
    }
}