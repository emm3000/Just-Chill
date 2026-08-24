package com.emm.justchill.hh.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.Eyebrow
import kotlinx.datetime.Month

/** Section label shared by every screen that surfaces pending recurring movements. */
@Composable
fun PendingRecurringHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eyebrow(text = "Pendientes")
    }
}

/** One pending recurring row; tapping it is how [ConfirmRecurringSheet] gets opened. */
@Composable
fun PendingRecurringRow(item: PendingRecurringUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = dropUnlessResumed(block = onClick),
            )
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    letterSpacing = (-0.15).sp,
                ),
                color = colors.textPrimary,
            )
            Text(
                text = if (item.isCatchUp) "Día ${item.dayOfMonth} · ${item.periodLabel}" else "Día ${item.dayOfMonth}",
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                ),
                color = if (item.isCatchUp) colors.danger else colors.textTertiary,
            )
        }
        Text(
            text = item.formattedAmount,
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = (-0.1).sp,
            ),
            color = when (item.type) {
                TransactionType.Income -> colors.success
                TransactionType.Spend -> colors.danger
            },
        )
    }
}

@Preview
@Composable
private fun PendingRecurringComponentsPreview() {
    EmmTheme {
        Column {
            PendingRecurringHeader(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp))
            PendingRecurringRow(item = PREVIEW_FIXED_ITEM, onClick = {})
            PendingRecurringRow(item = PREVIEW_CATCH_UP_ITEM, onClick = {})
        }
    }
}

private val PREVIEW_FIXED_ITEM = PendingRecurringUi(
    id = "rm-1@2026-08",
    templateId = "rm-1",
    period = YearMonth(2026, Month.AUGUST),
    periodLabel = "Agosto 2026",
    isCatchUp = false,
    name = "Netflix",
    type = TransactionType.Spend,
    formattedAmount = "-S/ 18.00",
    isVariableAmount = false,
    dayOfMonth = 15,
    accountId = "acc-1",
    categoryId = null,
    description = "",
    fixedAmountCents = 1800L,
)

private val PREVIEW_CATCH_UP_ITEM = PendingRecurringUi(
    id = "rm-2@2026-03",
    templateId = "rm-2",
    period = YearMonth(2026, Month.MARCH),
    periodLabel = "Marzo 2026",
    isCatchUp = true,
    name = "Alquiler",
    type = TransactionType.Spend,
    formattedAmount = "Variable",
    isVariableAmount = true,
    dayOfMonth = 1,
    accountId = "acc-1",
    categoryId = null,
    description = "",
    fixedAmountCents = null,
)
