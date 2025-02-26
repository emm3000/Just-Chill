package com.emm.justchill.hh.shared.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.transaction.presentation.TransactionType

@Composable
fun EmmTransactionRadioButton(
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
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = transactionType.value,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
            }
        }
    }

}

@PreviewLightDark
@Composable
fun RadioPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            EmmTransactionRadioButton()
        }
    }
}