package com.emm.justchill.experiences.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.TextColor
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun Calendar(
    onDayClick: (String) -> Unit = {},
) {

    val pagerState: PagerState = rememberPagerState(initialPage = 50, pageCount = { 100 })
    val currentDate = remember { mutableStateOf(LocalDate.now()) }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
    ) { page ->
        val monthOffset = page - 50
        val displayedMonth: LocalDate = remember(monthOffset) {
            currentDate.value.plusMonths(monthOffset.toLong())
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayedMonth.month.getDisplayName(TextStyle.FULL, Locale("es"))
                    .uppercase() + " " + displayedMonth.year,
                style = MaterialTheme.typography.headlineMedium,
                color = TextColor,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
            )

            CalendarView(displayedMonth) {

            }
        }
    }
}

@Composable
fun CalendarView(
    month: LocalDate,
    onDateClick: (LocalDate) -> Unit = {},
) {

    val calendarRows: List<List<LocalDate?>> = remember(month) {
        calculateDays(month)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {

        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")),
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextColor,
                    fontSize = 16.sp,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        val selectedDate = remember { mutableStateOf<LocalDate?>(null) }

        calendarRows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(
                                if (day != null && day == selectedDate.value) PrimaryButtonColor else Color.Transparent
                            )
                            .clickable {
                                day?.let {
                                    selectedDate.value = it
                                    onDateClick(it)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                color = TextColor,
                                fontFamily = LatoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
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
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Calendar()
        }
    }
}