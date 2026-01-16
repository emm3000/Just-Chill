package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi

@Composable
fun TransactionItem(
    modifier: Modifier = Modifier,
    transactionUi: TransactionUi,
) {

    val color = when (transactionUi.type) {
        TransactionType.Income -> MaterialTheme.colorScheme.onBackground
        TransactionType.Spend -> DeleteButtonColor
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(transactionUi.category.categoryColor.darkContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = transactionUi.category.categoryIcon,
                contentDescription = null,
                tint = transactionUi.category.categoryColor.primary
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = transactionUi.description,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = transactionUi.readableTime,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = LatoFontFamily,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = transactionUi.amount,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFontFamily,
        )
    }
}

@Preview
@Composable
private fun TransactionItemPreview() {
    EmmTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            ) {
                TransactionItem(
                    transactionUi = TransactionUi(
                        transactionId = "eget",
                        type = TransactionType.Income,
                        amount = "qualisque",
                        description = "ubique",
                        date = 9766,
                        readableDate = "iusto",
                        readableTime = "elementum",
                        category = CategoryUi(
                            categoryIcon = Icons.Rounded.Abc,
                            categoryColor = findById("green")
                        )

                    )
                )
                TransactionItem(
                    transactionUi = TransactionUi(
                        transactionId = "eget",
                        type = TransactionType.Income,
                        amount = "qualisque",
                        description = "ubique",
                        date = 9766,
                        readableDate = "iusto",
                        readableTime = "elementum",
                        category = CategoryUi(
                            categoryIcon = Icons.Rounded.Abc,
                            categoryColor = findById("pink")
                        )

                    )
                )
                TransactionItem(
                    transactionUi = TransactionUi(
                        transactionId = "eget",
                        type = TransactionType.Spend,
                        amount = "S/ 12.00",
                        description = "ubique",
                        date = 9766,
                        readableDate = "iusto",
                        readableTime = "elementum",
                        category = CategoryUi(
                            categoryIcon = Icons.Rounded.Abc,
                            categoryColor = findById("gray")
                        )

                    )
                )
            }
        }
    }
}