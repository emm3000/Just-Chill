package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun TotalAmountHero(totalFormatted: String, type: TransactionType, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val typeTokens = LocalEmmType.current

    val integerColor = when (type) {
        TransactionType.Income -> colors.success
        TransactionType.Spend -> colors.textPrimary
    }

    val annotated: AnnotatedString = buildAnnotatedString {
        val dotIndex = totalFormatted.lastIndexOf('.')
        val slashEnd = totalFormatted.indexOf(' ') + 1

        if (dotIndex < 0 || slashEnd <= 0) {
            withStyle(SpanStyle(color = integerColor)) {
                append(totalFormatted)
            }
        } else {
            withStyle(SpanStyle(color = colors.textTertiary, fontSize = typeTokens.amountL.fontSize)) {
                append(totalFormatted.substring(0, slashEnd))
            }
            withStyle(SpanStyle(color = integerColor)) {
                append(totalFormatted.substring(slashEnd, dotIndex))
            }
            withStyle(
                SpanStyle(
                    color = colors.textTertiary,
                    fontSize = (typeTokens.amountL.fontSize.value * 0.6f).sp,
                ),
            ) {
                append(totalFormatted.substring(dotIndex))
            }
        }
    }

    Text(
        text = annotated,
        style = typeTokens.amountL,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun TotalAmountHeroIncomeAndSpendPreview() {
    EmmTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TotalAmountHero(totalFormatted = "S/ 6,200.00", type = TransactionType.Income)
            TotalAmountHero(totalFormatted = "S/ 4,580.00", type = TransactionType.Spend)
        }
    }
}
