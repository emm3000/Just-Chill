package com.emm.justchill.hh.transaction.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.titlecaseFirstChar
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

/**
 * Bottom sheet for picking a date.
 *
 * Shortcut pills (Hoy / Ayer / Esta semana / Este mes) call [onConfirm] directly.
 * The day grid updates in-memory selection; only the CTA commits it.
 *
 * The sheet speaks [LocalDate] in both directions. It always did internally — it took epoch millis
 * only to convert them to a day on the first line and back on the last — and that round trip was
 * what let both call sites hand it `DateUtils.currentDateInMillis()` instead of the date actually
 * selected, so it reopened on today whatever the user had chosen.
 *
 * @param currentDate  The day currently selected, which the grid opens on.
 * @param onConfirm    Delivers the chosen day to the caller.
 * @param onDismiss    Dismisses the sheet.
 */
@Composable
fun DatePickerSheet(currentDate: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val zone = TimeZone.currentSystemDefault()
    val today: LocalDate = Clock.System.now().toLocalDateTime(zone).date

    var selectedDate: LocalDate by remember(currentDate) { mutableStateOf(currentDate) }
    // First day of the currently displayed month.
    var displayedMonth: LocalDate by remember(currentDate) { mutableStateOf(currentDate.firstOfMonth()) }

    val shortcuts: List<Shortcut> = remember(today) {
        listOf(
            Shortcut("Hoy", today),
            Shortcut("Ayer", today.minus(1, DateTimeUnit.DAY)),
            Shortcut("Esta semana", today.startOfWeekMonday()),
            Shortcut("Este mes", today.firstOfMonth()),
        )
    }

    val pillShape = RoundedCornerShape(999.dp)
    val activePillBg = colors.textPrimary
    val activePillFg = colors.bg
    val inactivePillFg = colors.textSecondary

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
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Selecciona fecha",
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
            )
            IconBtn(
                icon = Icons.Outlined.Close,
                onClick = onDismiss,
                modifier = Modifier.size(36.dp),
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(shortcuts) { shortcut ->
                val isActive = shortcut.label == "Hoy" && selectedDate == today

                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(if (isActive) activePillBg else Color.Transparent)
                        .border(BorderStroke(1.dp, if (isActive) activePillBg else colors.border), pillShape)
                        .clickable {
                            onConfirm(shortcut.date)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = shortcut.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = if (isActive) activePillFg else inactivePillFg,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(
                icon = Icons.Outlined.ChevronLeft,
                onClick = { displayedMonth = displayedMonth.minus(1, DateTimeUnit.MONTH) },
                modifier = Modifier.size(36.dp),
            )
            val monthLabel = remember(displayedMonth) {
                SpanishDateFormat.monthYear(displayedMonth.year, displayedMonth.month).titlecaseFirstChar()
            }
            Text(
                text = monthLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.15).sp,
            )
            IconBtn(
                icon = Icons.Outlined.ChevronRight,
                onClick = { displayedMonth = displayedMonth.plus(1, DateTimeUnit.MONTH) },
                modifier = Modifier.size(36.dp),
            )
        }

        // Weekday header — Monday-first, single-letter labels
        val weekdayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
        ) {
            weekdayLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = colors.textTertiary,
                    )
                }
            }
        }

        val days = remember(displayedMonth) { displayedMonth.daysGrid() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (week in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val date = days[week * 7 + col]
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (date != null) {
                                val isSelected = date == selectedDate
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.accent else Color.Transparent)
                                        .clickable { selectedDate = date },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.W500,
                                        fontFamily = InterFontFamily,
                                        color = if (isSelected) Color.White else colors.textPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val confirmShape = RoundedCornerShape(12.dp)
        val confirmLabel = remember(selectedDate) {
            SpanishDateFormat.dayFullMonth(selectedDate)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                .height(46.dp)
                .clip(confirmShape)
                .background(colors.textPrimary)
                .clickable {
                    onConfirm(selectedDate)
                    onDismiss()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Confirmar · $confirmLabel",
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.bg,
                letterSpacing = (-0.15).sp,
            )
        }
    }
}

private const val CALENDAR_GRID_CELLS = 42

private fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

/** Monday of the ISO week containing this date. */
private fun LocalDate.startOfWeekMonday(): LocalDate {
    val offset = (dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber + 7) % 7
    return minus(offset, DateTimeUnit.DAY)
}

private fun LocalDate.lengthOfMonth(): Int {
    val firstOfMonth = firstOfMonth()
    val firstOfNextMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH)
    return firstOfNextMonth.minus(1, DateTimeUnit.DAY).dayOfMonth
}

/** [this] must be the first day of the displayed month. */
private fun LocalDate.daysGrid(): List<LocalDate?> {
    val offset = (dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber + 7) % 7
    val length = lengthOfMonth()
    return List(CALENDAR_GRID_CELLS) { index ->
        val dayNumber = index - offset + 1
        if (dayNumber in 1..length) LocalDate(year, month, dayNumber) else null
    }
}
