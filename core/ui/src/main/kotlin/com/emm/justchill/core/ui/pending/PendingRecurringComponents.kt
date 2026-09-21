package com.emm.justchill.core.ui.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.atoms.CategoryDotSlot
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import kotlinx.datetime.Month

@Composable
fun PendingRecurringHeader(modifier: Modifier = Modifier) {
    Eyebrow(text = "Pendientes", modifier = modifier)
}

@Composable
fun PendingRecurringRow(item: PendingRecurringUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val rowBackground: Color = if (isPressed) colors.surface1 else Color.Transparent
    val dayOfMonthLabel: String = "Día ${item.dayOfMonth}"
    val dayLabel: String = if (item.isCatchUp) "$dayOfMonthLabel · ${item.periodLabel}" else dayOfMonthLabel

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = dropUnlessResumed(block = onClick),
            )
            .padding(horizontal = spacing.s6, vertical = spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = Icons.Outlined.Repeat, size = IconTileSize.Lg)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                CategoryDotSlot(color = null)
                Text(
                    text = item.name,
                    style = type.bodyM.copy(fontWeight = FontWeight.W500),
                    color = colors.textPrimary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                CategoryDotSlot(color = null)
                Text(
                    text = dayLabel,
                    style = type.caption,
                    color = if (item.isCatchUp) colors.danger else colors.textTertiary,
                )
            }
        }
        if (item.isVariableAmount) {
            Text(
                text = item.formattedAmount,
                style = type.bodyM,
                fontStyle = FontStyle.Italic,
                color = colors.textTertiary,
            )
        } else {
            Text(
                text = item.formattedAmount,
                style = type.amountM.copy(fontWeight = FontWeight.W600),
                color = if (item.type == TransactionType.Income) colors.success else colors.textPrimary,
            )
        }
    }
}

@Preview
@Composable
private fun PendingRecurringComponentsPreview() {
    EmmTheme {
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Column {
            PendingRecurringHeader(
                modifier = Modifier.padding(
                    start = spacing.s6,
                    end = spacing.s6,
                    top = spacing.s6,
                    bottom = spacing.s2,
                ),
            )
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
