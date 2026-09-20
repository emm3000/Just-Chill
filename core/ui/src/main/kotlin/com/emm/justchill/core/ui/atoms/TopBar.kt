package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.core.ui.theme.edgeGiveback

private const val LEFT_SLOT_ID: String = "left"
private const val TITLE_SLOT_ID: String = "title"
private const val RIGHT_SLOT_ID: String = "right"

@Composable
fun JcTopBar(
    title: String,
    modifier: Modifier = Modifier,
    left: @Composable (() -> Unit)? = null,
    right: @Composable (() -> Unit)? = null,
    rightArtwork: Dp = LocalEmmSpacing.current.s12,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val contentColumn: Dp = spacing.s4
    val backGiveback: Dp = spacing.edgeGiveback(spacing.s6)
    val startPadding: Dp = contentColumn - backGiveback
    val endPadding: Dp = contentColumn - spacing.edgeGiveback(rightArtwork)
    val titleColumnGap: Dp = backGiveback
    val titleGap: Dp = spacing.s2
    val slotSize: Dp = spacing.s12

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.s16)
            .padding(start = startPadding, end = endPadding),
        content = {
            if (left != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.layoutId(LEFT_SLOT_ID),
                ) {
                    left()
                }
            }

            Text(
                text = title,
                style = type.titleL,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.layoutId(TITLE_SLOT_ID),
            )

            if (right != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.layoutId(RIGHT_SLOT_ID),
                ) {
                    right()
                }
            }
        },
        measurePolicy = { measurables: List<Measurable>, constraints: Constraints ->
            measureTopBar(measurables, constraints, slotSize, titleColumnGap, titleGap)
        },
    )
}

private fun MeasureScope.measureTopBar(
    measurables: List<Measurable>,
    constraints: Constraints,
    slotSize: Dp,
    titleColumnGap: Dp,
    titleGap: Dp,
): MeasureResult {
    val barWidth: Int = constraints.maxWidth
    val barHeight: Int = constraints.maxHeight
    val minSlotSize: Int = slotSize.roundToPx()
    val slotConstraints = Constraints(
        minWidth = minSlotSize,
        maxWidth = barWidth,
        minHeight = minSlotSize.coerceAtMost(barHeight),
        maxHeight = barHeight,
    )

    val leftPlaceable: Placeable? = measurables
        .firstOrNull { it.layoutId == LEFT_SLOT_ID }
        ?.measure(slotConstraints)
    val rightPlaceable: Placeable? = measurables
        .firstOrNull { it.layoutId == RIGHT_SLOT_ID }
        ?.measure(slotConstraints)

    val titleStart: Int = leftPlaceable
        ?.let { it.width + titleGap.roundToPx() }
        ?: titleColumnGap.roundToPx()
    val titleEndGap: Int = rightPlaceable
        ?.let { it.width + titleGap.roundToPx() }
        ?: 0
    val titleWidth: Int = (barWidth - titleStart - titleEndGap).coerceAtLeast(0)
    val titlePlaceable: Placeable = measurables
        .first { it.layoutId == TITLE_SLOT_ID }
        .measure(
            Constraints(minWidth = 0, maxWidth = titleWidth, minHeight = 0, maxHeight = barHeight),
        )

    return layout(barWidth, barHeight) {
        leftPlaceable?.placeRelative(0, (barHeight - leftPlaceable.height) / 2)
        titlePlaceable.placeRelative(titleStart, (barHeight - titlePlaceable.height) / 2)
        rightPlaceable?.placeRelative(
            barWidth - rightPlaceable.width,
            (barHeight - rightPlaceable.height) / 2,
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun JcTopBarOverflowPreview() {
    EmmTheme {
        JcTopBar(
            title = "Cuidado personal y salud",
            left = { BackBtn(onClick = {}) },
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun JcTopBarOverflowWithActionsPreview() {
    EmmTheme {
        val spacing: EmmSpacing = LocalEmmSpacing.current
        JcTopBar(
            title = "Cuidado personal y salud",
            left = { BackBtn(onClick = {}) },
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    IconBtn(icon = Icons.Outlined.Edit, onClick = {}, contentDescription = "Editar")
                    IconBtn(icon = Icons.Outlined.Delete, onClick = {}, contentDescription = "Eliminar")
                }
            },
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun JcTopBarWithoutLeftPreview() {
    EmmTheme {
        JcTopBar(title = "Préstamos")
    }
}
