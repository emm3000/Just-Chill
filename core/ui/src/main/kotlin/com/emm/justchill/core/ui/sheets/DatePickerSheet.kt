package com.emm.justchill.core.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.emm.justchill.core.ui.atoms.FilledCta
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.format.SpanishDateFormat
import com.emm.justchill.core.ui.format.titlecaseFirstChar
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private data class Shortcut(val label: String, val date: LocalDate)

@Composable
fun DatePickerSheet(currentDate: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val zone: TimeZone = TimeZone.currentSystemDefault()
    val today: LocalDate = Clock.System.now().toLocalDateTime(zone).date

    var selectedDate: LocalDate by remember(currentDate) { mutableStateOf(currentDate) }
    var displayedMonth: LocalDate by remember(currentDate) { mutableStateOf(currentDate.firstOfMonth()) }

    val shortcuts: List<Shortcut> = remember(today) {
        listOf(
            Shortcut("Hoy", today),
            Shortcut("Ayer", today.minus(1, DateTimeUnit.DAY)),
            Shortcut("Esta semana", today.startOfWeekMonday()),
            Shortcut("Este mes", today.firstOfMonth()),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.s5, end = spacing.s5, bottom = spacing.s4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Selecciona fecha", style = type.titleM, color = colors.textPrimary)
                IconBtn(
                    icon = Icons.Outlined.Close,
                    onClick = onDismiss,
                    contentDescription = "Cerrar",
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.s5, end = spacing.s5, bottom = spacing.s4),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                items(shortcuts) { shortcut ->
                    ShortcutPill(
                        label = shortcut.label,
                        isActive = shortcut.label == "Hoy" && selectedDate == today,
                        onClick = {
                            onConfirm(shortcut.date)
                            onDismiss()
                        },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4, vertical = spacing.s3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(
                    icon = Icons.Outlined.ChevronLeft,
                    onClick = { displayedMonth = displayedMonth.minus(1, DateTimeUnit.MONTH) },
                    contentDescription = "Mes anterior",
                )
                val monthLabel: String = remember(displayedMonth) {
                    SpanishDateFormat.monthYear(displayedMonth.year, displayedMonth.month).titlecaseFirstChar()
                }
                Text(text = monthLabel, style = type.titleM, color = colors.textPrimary)
                // A transaction records money that already moved, so there is no month after this one
                // to browse. The domain rejects a future date outright (TransactionDateRules); this
                // chevron and the day cells' own enabled gate keep the user away from that error.
                val canGoForward: Boolean = displayedMonth < today.firstOfMonth()
                IconBtn(
                    icon = Icons.Outlined.ChevronRight,
                    onClick = { displayedMonth = displayedMonth.plus(1, DateTimeUnit.MONTH) },
                    contentDescription = "Mes siguiente",
                    enabled = canGoForward,
                )
            }

            val weekdayLabels: List<String> = listOf("L", "M", "M", "J", "V", "S", "D")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s5)
                    .padding(bottom = spacing.s2),
            ) {
                weekdayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = type.caption.copy(fontWeight = FontWeight.W500),
                            color = colors.textTertiary,
                        )
                    }
                }
            }

            val days: List<LocalDate?> = remember(displayedMonth) { displayedMonth.daysGrid() }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s5),
            ) {
                for (week in 0 until CALENDAR_WEEKS) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until DAYS_IN_WEEK) {
                            val date: LocalDate? = days[week * DAYS_IN_WEEK + col]
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isFuture = date != null && date > today,
                                onSelect = { if (date != null) selectedDate = date },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        val confirmLabel: String = remember(selectedDate) {
            SpanishDateFormat.dayFullMonth(selectedDate)
        }

        FilledCta(
            label = "Confirmar · $confirmLabel",
            onClick = {
                onConfirm(selectedDate)
                onDismiss()
            },
            modifier = Modifier.padding(start = spacing.s4, end = spacing.s4, bottom = spacing.s4, top = spacing.s2),
        )
    }
}

@Composable
private fun ShortcutPill(label: String, isActive: Boolean, onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val pillShape: RoundedCornerShape = LocalEmmRadii.current.rFull
    val pillBg: Color = if (isActive) colors.textPrimary else Color.Transparent
    val pillBorder: Color = if (isActive) pillBg else colors.border
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .height(spacing.s12)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(pillShape)
                .background(pillBg)
                .border(BorderStroke(spacing.hairline, pillBorder), pillShape)
                .indication(interactionSource, ripple())
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
        ) {
            Text(
                text = label,
                style = type.labelM,
                color = if (isActive) colors.bg else colors.textSecondary,
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isFuture: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    // Seven columns share the grid's 20dp gutters, so under ~376dp of width the slot cannot also be 48dp wide.
    Box(
        modifier = modifier.height(spacing.s12),
        contentAlignment = Alignment.Center,
    ) {
        if (date == null) return@Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !isFuture,
                    onClick = onSelect,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.s10)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.surface3 else Color.Transparent)
                    .indication(interactionSource, ripple()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = type.labelL,
                    color = when {
                        isSelected -> colors.textPrimary
                        isFuture -> colors.textTertiary
                        else -> colors.textPrimary
                    },
                )
            }
        }
    }
}

private const val CALENDAR_GRID_CELLS = 42
private const val CALENDAR_WEEKS: Int = 6
private const val DAYS_IN_WEEK: Int = 7

private fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

private fun LocalDate.startOfWeekMonday(): LocalDate {
    val offset: Int = (dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber + 7) % 7
    return minus(offset, DateTimeUnit.DAY)
}

private fun LocalDate.lengthOfMonth(): Int {
    val firstOfMonth: LocalDate = firstOfMonth()
    val firstOfNextMonth: LocalDate = firstOfMonth.plus(1, DateTimeUnit.MONTH)
    return firstOfNextMonth.minus(1, DateTimeUnit.DAY).dayOfMonth
}

/** [this] must be the first day of the displayed month. */
private fun LocalDate.daysGrid(): List<LocalDate?> {
    val offset: Int = (dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber + 7) % 7
    val length: Int = lengthOfMonth()
    return List(CALENDAR_GRID_CELLS) { index ->
        val dayNumber: Int = index - offset + 1
        if (dayNumber in 1..length) LocalDate(year, month, dayNumber) else null
    }
}
