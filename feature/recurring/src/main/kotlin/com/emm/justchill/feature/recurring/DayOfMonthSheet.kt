package com.emm.justchill.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.emm.justchill.core.ui.atoms.CtaHeight
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

private const val DAY_GRID_COLUMNS = 6
private const val MIN_DAY = 1
private const val MAX_DAY = 31

@Composable
fun DayOfMonthSheet(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val radii: EmmRadii = LocalEmmRadii.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableIntStateOf(current) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s6, end = spacing.s4, bottom = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "¿Qué día del mes?",
                style = type.titleM,
                color = colors.textPrimary,
            )
            val closeInteraction: MutableInteractionSource = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(spacing.s12)
                    .clickable(
                        interactionSource = closeInteraction,
                        indication = null,
                        onClick = onDismiss,
                    ),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(spacing.s8)
                        .clip(CircleShape)
                        .background(colors.surface1)
                        .border(spacing.hairline, colors.border, CircleShape)
                        .indication(closeInteraction, ripple()),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(spacing.s3),
                    )
                }
            }
        }

        DayGrid(
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s3),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(spacing.s3)
                    .padding(top = spacing.s1),
            )
            Text(
                text = "Si eliges 29–31 y el mes no llega a ese día, se usa el último día del mes.",
                style = type.caption,
                color = colors.textTertiary,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4)
                .padding(bottom = spacing.s4)
                .height(CtaHeight)
                .clip(radii.rL)
                .background(colors.textPrimary)
                .clickable {
                    onConfirm(selected)
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Text(
                    text = "Listo",
                    style = type.titleM,
                    color = colors.bg,
                )
                Text(
                    text = "·",
                    style = type.titleM,
                    color = colors.bg.copy(alpha = 0.6f),
                )
                Text(
                    text = "Día $selected",
                    style = type.titleM,
                    color = colors.bg.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun DayGrid(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val radii: EmmRadii = LocalEmmRadii.current
    val days = (MIN_DAY..MAX_DAY).toList()
    val rows = days.chunked(DAY_GRID_COLUMNS)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        rows.forEach { rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                rowDays.forEach { day ->
                    val isSelected = day == selected
                    val cellShape = radii.rXS
                    val cellBg = if (isSelected) colors.surface3 else Color.Transparent
                    val cellBorder: Color = if (isSelected) colors.borderFocus else colors.border
                    val textColor = if (isSelected) colors.textPrimary else colors.textSecondary

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(spacing.s12)
                            .clip(cellShape)
                            .background(cellBg)
                            .border(spacing.hairline, cellBorder, cellShape)
                            .clickable { onSelect(day) },
                    ) {
                        Text(
                            text = day.toString(),
                            style = type.labelL.copy(
                                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                            ),
                            color = textColor,
                        )
                    }
                }
                val remainder = DAY_GRID_COLUMNS - rowDays.size
                if (remainder > 0) {
                    repeat(remainder) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
