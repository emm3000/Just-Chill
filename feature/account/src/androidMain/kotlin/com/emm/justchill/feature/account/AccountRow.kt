package com.emm.justchill.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.EmmRowMenu
import com.emm.justchill.core.ui.atoms.EmmRowMenuGlyphSize
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.color
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.core.ui.theme.edgeGiveback

@Composable
internal fun AccountRow(row: AccountMonthUi, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.s6,
                    end = spacing.s6 - spacing.edgeGiveback(EmmRowMenuGlyphSize),
                    top = spacing.s4,
                    bottom = spacing.s4,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            AccountRowContent(
                modifier = Modifier.weight(1f),
                gap = spacing.s3,
                stackGap = spacing.s1,
                tile = { IconTile(icon = row.account.type.toIcon(), size = IconTileSize.Lg) },
                texts = {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
                        Text(text = row.account.name, style = type.titleM, color = colors.textPrimary)
                        Text(
                            text = accountSubtitle(row.account.type.toLabel(), row.movementCount),
                            style = type.labelM,
                            color = colors.textTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                net = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(spacing.s1),
                    ) {
                        Text(
                            text = row.net,
                            style = type.amountM,
                            color = row.netTone.toAmountTone().color(colors),
                            softWrap = false,
                        )
                        Text(text = "este mes", style = type.caption, color = colors.textTertiary)
                    }
                },
            )

            EmmRowMenu(contentDescription = "Opciones de cuenta", onEdit = onEdit, onDelete = onDelete)
        }
        Hairline()
    }
}

@Composable
private fun AccountRowContent(
    gap: Dp,
    stackGap: Dp,
    tile: @Composable () -> Unit,
    texts: @Composable () -> Unit,
    net: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(tile, texts, net),
        modifier = modifier,
    ) { measurables: List<List<Measurable>>, constraints: Constraints ->
        val gapPx: Int = gap.roundToPx()
        val tilePlaceable: Placeable = measurables[0].single().measure(Constraints())
        val netPlaceable: Placeable = measurables[2].single().measure(Constraints())
        val textsMeasurable: Measurable = measurables[1].single()
        val textsSpace: Int = (constraints.maxWidth - tilePlaceable.width - gapPx).coerceAtLeast(0)
        val besideSpace: Int = (textsSpace - gapPx - netPlaceable.width).coerceAtLeast(0)
        val fitsBeside: Boolean = textsMeasurable.maxIntrinsicWidth(Constraints.Infinity) <= besideSpace
        val textsWidth: Int = if (fitsBeside) besideSpace else textsSpace
        val textsPlaceable: Placeable = textsMeasurable.measure(
            Constraints(minWidth = textsWidth, maxWidth = textsWidth),
        )
        val textsX: Int = tilePlaceable.width + gapPx
        if (fitsBeside) {
            val height: Int = maxOf(tilePlaceable.height, textsPlaceable.height, netPlaceable.height)
            layout(constraints.maxWidth, height) {
                tilePlaceable.place(0, (height - tilePlaceable.height) / 2)
                textsPlaceable.place(textsX, (height - textsPlaceable.height) / 2)
                netPlaceable.place(constraints.maxWidth - netPlaceable.width, (height - netPlaceable.height) / 2)
            }
        } else {
            val topHeight: Int = maxOf(tilePlaceable.height, textsPlaceable.height)
            val netY: Int = topHeight + stackGap.roundToPx()
            layout(constraints.maxWidth, netY + netPlaceable.height) {
                tilePlaceable.place(0, 0)
                textsPlaceable.place(textsX, 0)
                netPlaceable.place(0, netY)
            }
        }
    }
}

internal fun accountSubtitle(typeLabel: String, movementCount: Int): String = when (movementCount) {
    0 -> "Sin movimientos este mes"
    1 -> "$typeLabel · 1 movimiento"
    else -> "$typeLabel · $movementCount movimientos"
}

internal fun AccountNetTone.toAmountTone(): AmountTone = when (this) {
    AccountNetTone.Muted -> AmountTone.Mute
    AccountNetTone.Positive -> AmountTone.Pos
    AccountNetTone.Neutral -> AmountTone.Neutral
}
