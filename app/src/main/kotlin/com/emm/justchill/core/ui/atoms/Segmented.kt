package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Generic segmented control. Renders options left-to-right inside a pill-rounded
 * container with no inner dividers — the selected cell carries the visual weight.
 *
 * Visual tokens (Notion-style, subtle): 44dp height, `surface1` track bg with 1dp
 * border, selected cell = `surface2` filled with `textPrimary`; unselected =
 * transparent with `textSecondary`. Matches the designer's handoff for Reporte
 * tabs and the Ingresos/Gastos toggle.
 */
@Composable
fun <T> Segmented(options: List<SegmentOption<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(colors.surface1)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            SegmentCell(
                label = option.label,
                isSelected = isSelected,
                onClick = { onSelect(option.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

data class SegmentOption<T>(val value: T, val label: String)

@Composable
private fun SegmentCell(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val cellShape = RoundedCornerShape(10.dp)

    val bg: Color = if (isSelected) colors.surface2 else Color.Transparent
    val textColor: Color = if (isSelected) colors.textPrimary else colors.textSecondary

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(cellShape)
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = type.labelL,
            color = textColor,
        )
    }
}
