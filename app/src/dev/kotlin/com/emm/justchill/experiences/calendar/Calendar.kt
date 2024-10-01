package com.emm.justchill.experiences.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.loans.domain.Payment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@Composable
fun Calendar(
    payments: Map<String, List<Payment>> = emptyMap(),
    onDayClick: (String) -> Unit = {},
) {

    val pagerState: PagerState =
        rememberPagerState(initialPage = 50, pageCount = { 100 })
    val currentDate = remember { mutableStateOf(LocalDate.now()) }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth()
            .background(BackgroundColor)
    ) { page ->
        val monthOffset = page - 50
        val displayedMonth: LocalDate = currentDate.value.plusMonths(monthOffset.toLong())

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayedMonth.month.name + " " + displayedMonth.year,
                style = MaterialTheme.typography.headlineMedium,
                color = TextColor,
            )

            CalendarView(displayedMonth)
        }
    }
}

@Composable
fun CalendarView(
    month: LocalDate,
    markedDates: List<LocalDate> = emptyList(),
    onDateClick: (LocalDate) -> Unit = {},
) {
    val currentMonth: YearMonth = YearMonth.from(month)
    val firstDayOfMonth: LocalDate = currentMonth.atDay(1)
    val lastDayOfMonth: LocalDate = currentMonth.atEndOfMonth()

    val daysOfWeek: List<DayOfWeek> = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    )

    val totalDays: List<LocalDate> = (1..lastDayOfMonth.dayOfMonth).map { currentMonth.atDay(it) }

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {

        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextColor,
                )
            }
        }

        val startOffset: Int = firstDayOfMonth.dayOfWeek.value % 7

        val calendarRows: List<List<LocalDate?>> = (0 until 5).map { weekIndex ->
            val startDay: Int = weekIndex * 7 - startOffset + 1
            (startDay..(startDay + 6)).map { day ->
                totalDays.getOrNull(day - 1)
            }
        }

        calendarRows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(
                                if (day != null && markedDates.contains(day)) Color.Green else Color.Transparent
                            )
                            .clickable { day?.let { onDateClick(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                color = TextColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarPreview() {
    EmmTheme {
        Calendar()
    }
}