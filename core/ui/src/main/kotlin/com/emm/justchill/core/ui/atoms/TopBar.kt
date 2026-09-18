package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.InterFontFamily
import com.emm.justchill.core.ui.theme.LocalEmmColors

private val MinActionSlotSize: Dp = 48.dp
private const val LEFT_SLOT_ID: String = "left"
private const val TITLE_SLOT_ID: String = "title"
private const val RIGHT_SLOT_ID: String = "right"

@Composable
fun JcTopBar(
    title: String,
    modifier: Modifier = Modifier,
    left: @Composable (() -> Unit)? = null,
    right: @Composable (() -> Unit)? = null,
) {
    val colors = LocalEmmColors.current

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp),
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
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
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
            measureTopBar(measurables, constraints)
        },
    )
}

private fun MeasureScope.measureTopBar(
    measurables: List<Measurable>,
    constraints: Constraints,
): MeasureResult {
    val barWidth: Int = constraints.maxWidth
    val barHeight: Int = constraints.maxHeight
    val minSlotSize: Int = MinActionSlotSize.roundToPx()
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

    val sideWidth: Int = maxOf(leftPlaceable?.width ?: 0, rightPlaceable?.width ?: 0)
    val titleWidth: Int = (barWidth - 2 * sideWidth).coerceAtLeast(0)
    val titlePlaceable: Placeable = measurables
        .first { it.layoutId == TITLE_SLOT_ID }
        .measure(
            Constraints(minWidth = 0, maxWidth = titleWidth, minHeight = 0, maxHeight = barHeight),
        )

    return layout(barWidth, barHeight) {
        leftPlaceable?.placeRelative(0, (barHeight - leftPlaceable.height) / 2)
        titlePlaceable.placeRelative(
            (barWidth - titlePlaceable.width) / 2,
            (barHeight - titlePlaceable.height) / 2,
        )
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
            left = {
                IconBtn(icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = {}, contentDescription = "Volver")
            },
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun JcTopBarOverflowWithActionsPreview() {
    EmmTheme {
        JcTopBar(
            title = "Cuidado personal y salud",
            left = {
                IconBtn(icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = {}, contentDescription = "Volver")
            },
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBtn(icon = Icons.Outlined.Edit, onClick = {}, contentDescription = "Editar")
                    IconBtn(icon = Icons.Outlined.Delete, onClick = {}, contentDescription = "Eliminar")
                }
            },
        )
    }
}
