package com.emm.justchill.hh.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.SheetDragHandle

private const val DAY_GRID_COLUMNS = 7
private const val MIN_DAY = 1
private const val MAX_DAY = 31

/**
 * Bottom sheet for picking a day of month (1–31).
 *
 * Renders a 7-column grid. Selected day is highlighted with accent background.
 * A note below the grid explains that days 29–31 are clamped to the last day of the
 * month when needed — this mirrors [RecurringDueRules.effectiveDueDay] in the domain layer.
 */
@Composable
fun DayOfMonthSheet(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableIntStateOf(current) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "¿Qué día del mes?",
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.15).sp,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surface1)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(onClick = onDismiss),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        // Day grid: 7 columns, days 1..31
        DayGrid(
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        // Note row explaining clamping behavior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(13.dp)
                    .padding(top = 2.dp),
            )
            Text(
                text = "Si eliges 29–31 y el mes no llega a ese día, se usa el último día del mes.",
                fontSize = 11.sp,
                fontWeight = FontWeight.W400,
                fontFamily = InterFontFamily,
                color = colors.textTertiary,
            )
        }

        // Confirm button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accent)
                .clickable { onConfirm(selected) },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Listo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textOnAccent,
                    letterSpacing = (-0.15).sp,
                )
                Text(
                    text = "·",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textOnAccent.copy(alpha = 0.6f),
                )
                Text(
                    text = "Día $selected",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textOnAccent.copy(alpha = 0.9f),
                    letterSpacing = (-0.15).sp,
                )
            }
        }
    }
}

@Composable
private fun DayGrid(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val days = (MIN_DAY..MAX_DAY).toList()
    val rows = days.chunked(DAY_GRID_COLUMNS)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowDays.forEach { day ->
                    val isSelected = day == selected
                    val cellShape = RoundedCornerShape(8.dp)
                    val cellBg = if (isSelected) colors.accent else Color.Transparent
                    val cellBorder = if (isSelected) colors.accent else colors.border
                    val textColor = if (isSelected) colors.textOnAccent else colors.textSecondary

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(cellShape)
                            .background(cellBg)
                            .border(1.dp, cellBorder, cellShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(day) },
                            ),
                    ) {
                        Text(
                            text = day.toString(),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                            fontFamily = InterFontFamily,
                            color = textColor,
                        )
                    }
                }
                // Fill empty cells in the last row so columns align
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
