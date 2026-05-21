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
import androidx.compose.foundation.layout.width
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

data class SegmentOption<T>(val value: T, val label: String)

/**
 * Generic segmented control. Renders options left-to-right with a border
 * around the group and vertical dividers between segments.
 *
 * Visual tokens: 40dp height, border, selected = accent bg + textOnAccent,
 * unselected = transparent bg + textSecondary. Matches ToggleIncomeExpense.
 */
@Composable
fun <T> Segmented(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .border(width = 1.dp, color = colors.border, shape = shape),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option.value == selected

            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.border),
                )
            }

            SegmentCell(
                label = option.label,
                isSelected = isSelected,
                onClick = { onSelect(option.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentCell(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val bg: Color = if (isSelected) colors.accent else Color.Transparent
    val textColor: Color = if (isSelected) colors.textOnAccent else colors.textSecondary

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
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
