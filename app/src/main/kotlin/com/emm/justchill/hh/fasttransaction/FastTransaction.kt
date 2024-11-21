package com.emm.justchill.hh.fasttransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.shared.shared.EmmPrimaryButton
import com.emm.justchill.hh.transaction.presentation.DateUtils
import com.emm.justchill.hh.transaction.presentation.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.presentation.TransactionType

@Composable
fun FastTransaction(
    transactionType: TransactionType,
    amountValue: TextFieldValue,
    onAmountChange: (TextFieldValue) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isEnabledButton: Boolean,
    addTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        EmmCenteredToolbar(
            title = "Agregar ${transactionType.value}"
        )

        EmmBoldText(
            text = remember { DateUtils.currentDateAtReadableFormat() },
            modifier = Modifier.fillMaxWidth()
        )

        EmmBoldText(
            text = "Ingrese un monto",
            modifier = Modifier.fillMaxWidth(),
        )

        EmmAmountChill(
            value = amountValue,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = description,
            onValueChange = onDescriptionChange,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            label = {
                Text(
                    text = "Descripción",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = LatoFontFamily,
                )
            },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface
            ),
            placeholder = {
                Text(
                    text = "Descripción",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontFamily = LatoFontFamily,
                )
            }
        )

        EmmPrimaryButton(
            text = "Guardar",
            onClick = addTransaction,
            enabled = isEnabledButton,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@PreviewLightDark
@Composable
private fun FastTransactionPreview() {
    EmmTheme {
        FastTransaction(
            transactionType = TransactionType.INCOME,
            addTransaction = {},
            amountValue = TextFieldValue("123"),
            onAmountChange = {},
            description = "",
            onDescriptionChange = {},
            isEnabledButton = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}