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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.emm.justchill.hh.transaction.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// TODO SR-10: replace Material3 DatePicker with a custom calendar grid matching the
//   SheetDate design (7-col grid, accent-filled today circle, no mode-toggle chrome).

/**
 * Bottom sheet for picking a date.
 *
 * Uses Material3 [DatePicker] as a pragmatic fallback for SR-3. A custom calendar
 * grid matching the JSX SheetDate design is deferred to SR-10.
 *
 * Shortcut pills (Hoy / Ayer / Esta semana / Este mes) call [onConfirm] directly and
 * dismiss the sheet without needing the Material picker.
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
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentMillis,
    )

    // Shortcuts
    data class Shortcut(val label: String, val millis: Long)

    val shortcuts: List<Shortcut> = remember(today) {
        val zone = ZoneId.systemDefault()
        listOf(
            Shortcut("Hoy", today.atStartOfDay(zone).toInstant().toEpochMilli()),
            Shortcut("Ayer", today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()),
            Shortcut(
                "Esta semana",
                today.with(java.time.DayOfWeek.MONDAY).atStartOfDay(zone).toInstant().toEpochMilli(),
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
        // Shortcuts row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(shortcuts) { shortcut ->
                val isActive = shortcut.label == "Hoy" &&
                    datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == today
                    } ?: false

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

        // Material3 DatePicker (SR-3 fallback)
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = colors.bg,
                selectedDayContainerColor = colors.accent,
                selectedDayContentColor = Color.White,
                todayContentColor = colors.accent,
                todayDateBorderColor = colors.accent,
                dayContentColor = colors.textPrimary,
                weekdayContentColor = colors.textTertiary,
                navigationContentColor = colors.textSecondary,
                yearContentColor = colors.textPrimary,
                selectedYearContainerColor = colors.accent,
                selectedYearContentColor = Color.White,
                subheadContentColor = colors.textPrimary,
                headlineContentColor = colors.textPrimary,
                titleContentColor = colors.textTertiary,
                dividerColor = colors.border,
                disabledDayContentColor = colors.textDisabled,
            ),
        )

        // Confirm CTA
        val confirmShape = RoundedCornerShape(12.dp)
        val selectedDate: String = remember(datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let { millis ->
                val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("es"))
                date.format(formatter)
            } ?: "—"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                .height(46.dp)
                .clip(confirmShape)
                .background(colors.textPrimary)
                .clickable {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Material DatePicker returns UTC midnight; convert to device-zone start-of-day
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        val deviceMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onConfirm(deviceMillis)
                    }
                    onDismiss()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Confirmar · $selectedDate",
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.bg,
            )
        }
    }
}
