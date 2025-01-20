package com.emm.justchill.hh.shared.shared

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhOnBackgroundColor
import com.emm.justchill.hh.auth.presentation.LabelTextField

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
            .background(HhBackgroundColor)
    ) {

        val (isExpanded, setIsExpanded) = remember {
            mutableStateOf(false)
        }

        val sizeInside by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = tween(easing = LinearOutSlowInEasing),
            label = ""
        )

        val selected: String by remember(itemSelected) {
            mutableStateOf(itemSelected?.toString() ?: "")
        }

        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = setIsExpanded,
        ) {
            TextField(
                value = selected,
                onValueChange = {  },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                placeholder = {
                    LabelTextField(textPlaceholder)
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = HhOnBackgroundColor,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HhOnBackgroundColor,
                ),
                suffix = {
                    Icon(
                        modifier = Modifier
                            .rotate(sizeInside),
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = HhOnBackgroundColor,
                    )
                },
                label = {
                    Text(
                        text = textLabel,
                        color = HhOnBackgroundColor,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
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
                modifier = Modifier
                    .exposedDropdownSize()
                    .background(HhBackgroundColor)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        modifier = Modifier,
                        text = {
                            Text(
                                text = item.toString(),
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = HhOnBackgroundColor,
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

@Preview(showBackground = true)
@Composable
private fun DropDownContainerPreview() {
    EmmTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
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