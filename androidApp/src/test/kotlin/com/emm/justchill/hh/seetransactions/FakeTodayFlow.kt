package com.emm.justchill.hh.seetransactions

import com.emm.justchill.core.time.TodayFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/**
 * Stands in for `ClockTodayFlow` so a rollover test drives [dates] directly instead of fighting
 * virtual time — collecting the real one inside `runTest` never terminates, because its
 * self-rescheduling `delay` shares the test scheduler. `ClockTodayFlowTest` pins the midnight
 * arithmetic this fake skips.
 */
internal class FakeTodayFlow(private val dates: MutableStateFlow<LocalDate>) : TodayFlow {

    override fun today(): LocalDate = dates.value

    override fun invoke(): Flow<LocalDate> = dates
}
