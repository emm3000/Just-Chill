package com.emm.justchill.hh.shared.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.transaction.presentation.TransactionType
import com.emm.justchill.hh.transaction.presentation.TransactionUi

@Composable
fun ItemTransaction(
    transactionUi: TransactionUi,
    navigateToEdit: (String) -> Unit,
) {

    val borderColor = when (transactionUi.type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.onBackground
        TransactionType.SPENT -> DeleteButtonColor
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable {
                navigateToEdit(transactionUi.transactionId)
            }
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transactionUi.description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = LatoFontFamily
                )
                Text(
                    modifier = Modifier,
                    text = "${transactionUi.readableDate}, ${transactionUi.readableTime}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = TextUnit(1f, TextUnitType.Em),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .8f),
                    fontFamily = LatoFontFamily
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 13.dp),
                text = transactionUi.amount,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = borderColor,
                fontFamily = LatoFontFamily
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }
}

@PreviewLightDark
@Composable
private fun ItemTransactionPreview() {
    EmmTheme {
        ItemTransaction(
            transactionUi = TransactionUi(
                transactionId = "quisque",
                type = TransactionType.INCOME,
                amount = "20.00",
                description = "voluptatibus",
                date = 8268,
                readableDate = "20/20",
                readableTime = "10 am"
            )
        ) { }
    }
}