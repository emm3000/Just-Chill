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
                name = {
                    Text(
                        text = row.account.name,
                        style = type.titleM,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                subtitle = {
                    Text(
                        text = accountSubtitle(row.account.type.toLabel(), row.movementCount),
                        style = type.labelM,
                        color = colors.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
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
    name: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    net: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(tile, name, subtitle, net),
        modifier = modifier,
    ) { measurables: List<List<Measurable>>, constraints: Constraints ->
        val gapPx: Int = gap.roundToPx()
        val stackGapPx: Int = stackGap.roundToPx()
        val tilePlaceable: Placeable = measurables[0].single().measure(Constraints())
        val subtitleMeasurable: Measurable = measurables[2].single()
        val netPlaceable: Placeable = measurables[3].single().measure(Constraints())
        val textsSpace: Int = (constraints.maxWidth - tilePlaceable.width - gapPx).coerceAtLeast(0)
        val placement: BesideOrStacked = accountNetPlacement(
            subtitleWidth = subtitleMeasurable.maxIntrinsicWidth(Constraints.Infinity),
            netWidth = netPlaceable.width,
            gap = gapPx,
            textsSpace = textsSpace,
        )
        val textsWidth: Int = when (placement) {
            BesideOrStacked.Beside -> (textsSpace - gapPx - netPlaceable.width).coerceAtLeast(0)
            BesideOrStacked.Stacked -> textsSpace
        }
        val textsConstraints: Constraints = Constraints(maxWidth = textsWidth)
        val namePlaceable: Placeable = measurables[1].single().measure(textsConstraints)
        val subtitlePlaceable: Placeable = subtitleMeasurable.measure(textsConstraints)
        val textsHeight: Int = namePlaceable.height + stackGapPx + subtitlePlaceable.height
        val textsX: Int = tilePlaceable.width + gapPx
        when (placement) {
            BesideOrStacked.Beside -> {
                val height: Int = maxOf(tilePlaceable.height, textsHeight, netPlaceable.height)
                val centered: Alignment.Vertical = Alignment.CenterVertically
                val textsY: Int = centered.align(textsHeight, height)
                layout(constraints.maxWidth, height) {
                    tilePlaceable.placeRelative(0, centered.align(tilePlaceable.height, height))
                    namePlaceable.placeRelative(textsX, textsY)
                    subtitlePlaceable.placeRelative(textsX, textsY + namePlaceable.height + stackGapPx)
                    netPlaceable.placeRelative(
                        constraints.maxWidth - netPlaceable.width,
                        centered.align(netPlaceable.height, height),
                    )
                }
            }

            BesideOrStacked.Stacked -> {
                val netY: Int = maxOf(tilePlaceable.height, textsHeight) + stackGapPx
                layout(constraints.maxWidth, netY + netPlaceable.height) {
                    tilePlaceable.placeRelative(0, 0)
                    namePlaceable.placeRelative(textsX, 0)
                    subtitlePlaceable.placeRelative(textsX, namePlaceable.height + stackGapPx)
                    netPlaceable.placeRelative(0, netY)
                }
            }
        }
    }
}

internal enum class BesideOrStacked { Beside, Stacked }

internal fun accountNetPlacement(subtitleWidth: Int, netWidth: Int, gap: Int, textsSpace: Int): BesideOrStacked =
    if (subtitleWidth + gap + netWidth <= textsSpace) BesideOrStacked.Beside else BesideOrStacked.Stacked

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
