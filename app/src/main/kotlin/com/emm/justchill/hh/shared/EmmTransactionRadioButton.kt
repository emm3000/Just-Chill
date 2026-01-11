package com.emm.justchill.hh.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme

@Composable
fun EmmTransactionRadioButton(
    modifier: Modifier = Modifier,
    selectedOption: TransactionType = TransactionType.Income,
    onOptionSelected: (TransactionType) -> Unit = {},
) {

    val transactionTypes: List<TransactionType> = remember {
        TransactionType.entries
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        transactionTypes.forEach { transactionType ->
            FilterChip(
                selected = selectedOption == transactionType,
                onClick = { onOptionSelected(transactionType) },
                label = {
                    Text(
                        text = transactionType.label,
                    )
                }
            )
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