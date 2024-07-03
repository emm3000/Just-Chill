package com.emm.justchill.experiences.hh.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.emm.justchill.experiences.hh.domain.TransactionType

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
                    color = selectedColor
                )
            }
        }
    }
}