package com.emm.justchill.hh.seetransactions

import com.emm.justchill.core.time.TodayFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/**
 * Drives the date by hand; `presentation/CLAUDE.md` says why a ViewModel test never collects the
 * real `ClockTodayFlow`.
 */
internal class FakeTodayFlow(private val dates: MutableStateFlow<LocalDate>) : TodayFlow {

    override fun today(): LocalDate = dates.value

    override fun invoke(): Flow<LocalDate> = dates
}
