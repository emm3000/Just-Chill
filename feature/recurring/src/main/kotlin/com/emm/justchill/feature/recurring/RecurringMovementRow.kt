package com.emm.justchill.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.atoms.EmmRowMenu
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
private fun DayBadge(dayOfMonth: Int) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val shape: RoundedCornerShape = LocalEmmRadii.current.rXS

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spacing.s8)
            .clip(shape)
            .background(colors.surface2)
            .border(spacing.hairline, colors.border, shape)
            .clearAndSetSemantics { contentDescription = "Día $dayOfMonth" },
    ) {
        Text(
            text = dayOfMonth.toString().padStart(2, '0'),
            style = type.labelM,
            color = colors.textPrimary,
        )
    }
}

@Composable
internal fun RecurringMovementRow(
    item: RecurringMovementUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    trailingBadge: (@Composable () -> Unit)? = null,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            DayBadge(item.dayOfMonth)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = type.titleM,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(spacing.s1))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.categoryColor?.let { colorKey ->
                        val dotColor: Color = colors.resolvedColor(colorKey)
                        Box(
                            modifier = Modifier
                                .size(spacing.s2)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                        Spacer(Modifier.width(spacing.s1))
                        Text(
                            text = "${item.categoryName} · ${item.accountName}",
                            style = type.labelM,
                            color = colors.textTertiary,
                        )
                    } ?: run {
                        Text(
                            text = item.accountName,
                            style = type.labelM,
                            color = colors.textTertiary,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.isVariableAmount) {
                    Text(
                        text = "Variable",
                        style = type.bodyM,
                        fontStyle = FontStyle.Italic,
                        color = colors.textTertiary,
                    )
                } else {
                    val amountColor: Color =
                        if (item.type == TransactionType.Income) colors.success else colors.textPrimary
                    Text(
                        text = item.formattedAmount,
                        style = type.amountS,
                        color = amountColor,
                    )
                }
                trailingBadge?.let {
                    Spacer(Modifier.height(spacing.s1))
                    it()
                }
            }

            EmmRowMenu(contentDescription = "Opciones", onEdit = onEdit, onDelete = onDelete)
        }
        Hairline()
    }
}
