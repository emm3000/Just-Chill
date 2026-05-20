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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale.forLanguageTag("es")

/**
 * Bottom sheet for picking a date.
 *
 * Shortcut pills (Hoy / Ayer / Esta semana / Este mes) call [onConfirm] directly.
 * The day grid updates in-memory selection; only the CTA commits it.
 *
 * @param currentMillis  Currently selected date in epoch-millis (device time-zone).
 * @param onConfirm      Delivers the selected epoch-millis to the caller.
 * @param onDismiss      Dismisses the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    currentMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val today: LocalDate = LocalDate.now()

    val initialDate: LocalDate = remember(currentMillis) {
        Instant.ofEpochMilli(currentMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    var selectedDate: LocalDate by remember { mutableStateOf(initialDate) }
    var displayedMonth: YearMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    // Shortcuts
    data class Shortcut(val label: String, val millis: Long)

    val shortcuts: List<Shortcut> = remember(today) {
        val zone = ZoneId.systemDefault()
        listOf(
            Shortcut("Hoy", today.atStartOfDay(zone).toInstant().toEpochMilli()),
            Shortcut("Ayer", today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()),
            Shortcut(
                "Esta semana",
                today.with(DayOfWeek.MONDAY).atStartOfDay(zone).toInstant().toEpochMilli(),
            ),
            Shortcut(
                "Este mes",
                today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            ),
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
        // Sheet header
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

        // Shortcuts row
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
                            onConfirm(shortcut.millis)
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

        // Month navigator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(
                icon = Icons.Outlined.ChevronLeft,
                onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                modifier = Modifier.size(36.dp),
            )
            val monthLabel = remember(displayedMonth) {
                val raw = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", ES))
                raw.replaceFirstChar { it.titlecase(ES) }
            }
            Text(
                text = monthLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
            )
            IconBtn(
                icon = Icons.Outlined.ChevronRight,
                onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                modifier = Modifier.size(36.dp),
            )
        }

        // Weekday header — lunes-first, Spanish single letter
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

        // Day grid — 6 rows × 7 columns
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

        // Confirm CTA
        val confirmShape = RoundedCornerShape(12.dp)
        val confirmLabel = remember(selectedDate) {
            val formatter = DateTimeFormatter.ofPattern("d MMMM", ES)
            selectedDate.format(formatter)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                .height(46.dp)
                .clip(confirmShape)
                .background(colors.textPrimary)
                .clickable {
                    val millis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onConfirm(millis)
                    onDismiss()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Confirmar · $confirmLabel",
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.bg,
            )
        }
    }
}

/** Returns 42 entries (6 rows × 7 cols, Mon-first). Cells outside the month are null. */
private fun YearMonth.daysGrid(): List<LocalDate?> {
    val firstDay = atDay(1)
    // Monday = 0 … Sunday = 6
    val offset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val length = lengthOfMonth()
    return List(42) { index ->
        val dayNumber = index - offset + 1
        if (dayNumber in 1..length) atDay(dayNumber) else null
    }
}
