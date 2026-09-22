package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun <T> Segmented(options: List<SegmentOption<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val shape: Shape = LocalEmmRadii.current.rM

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.s12)
            .clip(shape)
            .border(width = spacing.hairline, color = colors.border, shape = shape)
            .padding(horizontal = CELL_INSET),
    ) {
        options.forEach { option ->
            val isSelected: Boolean = option.value == selected
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
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val cellShape: Shape = radii.rS

    val bg: Color = if (isSelected) colors.surface2 else Color.Transparent
    val textColor: Color = if (isSelected) colors.textPrimary else colors.textSecondary

    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = CELL_INSET),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cellShape)
                .background(bg)
                .padding(horizontal = spacing.s3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = type.labelL,
                color = textColor,
            )
        }
    }
}

// The track's rM less the cell's rS, plus the hairline: keeps the selected cell's corners concentric with the track's.
private val CELL_INSET: Dp = 3.dp

@Preview
@PreviewRedmi15CWidth
@Composable
private fun SegmentedPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Column(modifier = Modifier.background(colors.bg).padding(spacing.s4)) {
            Segmented(
                options = listOf(
                    SegmentOption(value = "expense", label = "Gasto"),
                    SegmentOption(value = "income", label = "Ingreso"),
                ),
                selected = "expense",
                onSelect = {},
            )
        }
    }
}
