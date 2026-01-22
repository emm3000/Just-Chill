package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi

@Composable
fun ItemTransaction(
    transactionUi: TransactionUi,
    navigateToEdit: (String) -> Unit,
) {

    val borderColor = when (transactionUi.type) {
        TransactionType.Income -> MaterialTheme.colorScheme.onBackground
        TransactionType.Spend -> DeleteButtonColor
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navigateToEdit(transactionUi.transactionId) }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Surface(
                    shape = CircleShape,
                    color = transactionUi.category.categoryColor.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = transactionUi.category.categoryIcon,
                            contentDescription = null,
                            tint = transactionUi.category.categoryColor.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                // Info Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transactionUi.description,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = LatoFontFamily
                    )
                    Text(
                        text = "${transactionUi.readableDate} • ${transactionUi.readableTime}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontFamily = LatoFontFamily
                    )
                }

                // Amount
                Text(
                    text = transactionUi.amount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = borderColor,
                    fontFamily = LatoFontFamily,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ItemTransactionPreview() {
    EmmTheme {
        ItemTransaction(
            transactionUi = TransactionUi(
                transactionId = "quisque",
                type = TransactionType.Income,
                amount = "20.00",
                description = "voluptatibus",
                date = 8268,
                readableDate = "20/20",
                readableTime = "10 am",
                category = CategoryUi(
                    categoryIcon = Icons.Rounded.Category,
                    categoryColor = findById("gray")
                )
            )
        ) { }
    }
}