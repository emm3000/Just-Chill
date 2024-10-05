package com.emm.justchill.hh.shared.shared

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.transaction.presentation.TransactionType

@Composable
fun TransactionRadioButton(
    modifier: Modifier = Modifier,
    selectedOption: TransactionType = TransactionType.INCOME,
    onOptionSelected: (TransactionType) -> Unit = {},
) {

    val transactionTypes: List<TransactionType> = remember {
        TransactionType.entries
    }

    Row(
        modifier = modifier,
    ) {

        transactionTypes.forEach { transactionType ->
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .selectable(
                        selected = transactionType == selectedOption,
                        onClick = { onOptionSelected(transactionType) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = transactionType == selectedOption,
                    onClick = { onOptionSelected(transactionType) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = PrimaryButtonColor
                    )
                )
                Text(
                    text = transactionType.value,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = TextColor,
                    fontSize = 18.sp
                )
            }
        }
    }

}

@Composable
fun TransactionTypeRadioButton(
    selectedOption: TransactionType = TransactionType.INCOME,
    onOptionSelected: (TransactionType) -> Unit = {},
) {
    val transactionTypes: List<TransactionType> = TransactionType.entries

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        transactionTypes.forEach { transactionType: TransactionType ->

            val modifier: Modifier = if (transactionType == selectedOption) {
                Modifier.border(1.dp, transactionType.color)
            } else {
                Modifier.border(1.dp, Color.White)
            }

            val selectedColor: Color = if (transactionType == selectedOption) {
                transactionType.color
            } else {
                Color.Black
            }

            Row(
                modifier = Modifier
                    .selectableGroup()
                    .then(modifier)
                    .selectable(
                        selected = transactionType == selectedOption,
                        onClick = { onOptionSelected(transactionType) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 10.dp, horizontal = 20.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = selectedColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = transactionType.value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = selectedColor,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LatoFontFamily
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RadioPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TransactionRadioButton()
            TransactionTypeRadioButton()
        }
    }
}