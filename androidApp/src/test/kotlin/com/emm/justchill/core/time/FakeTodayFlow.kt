package com.emm.justchill.core.time

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

// Drives the date by hand, not through zone-correct `ClockTodayFlow` (pinned in ClockTodayFlowTest;
// `presentation/CLAUDE.md` says why a ViewModel test never collects the real one). A ViewModel's
// injected Clock still only answers "what hour", never "what day".
internal class FakeTodayFlow(private val dates: MutableStateFlow<LocalDate>) : TodayFlow {

    override fun today(): LocalDate = dates.value

    override fun invoke(): Flow<LocalDate> = dates
}
