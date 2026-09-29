package com.emm.justchill.core.di

import com.emm.justchill.core.DefaultUniqueIdProvider
import com.emm.justchill.core.domain.shared.UniqueIdProvider
import com.emm.justchill.core.domain.time.ClockTodayFlow
import com.emm.justchill.core.domain.time.TodayFlow
import com.emm.justchill.core.lifecycle.resumeEvents
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule = module {
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    // resumeEvents() is supplied here rather than called inside the class, so ClockTodayFlow holds
    // no platform seam and a test can hand it a plain flow.
    factory<TodayFlow> { ClockTodayFlow(clock = get(), zone = get(), resumeEvents = resumeEvents()) }

    // These two factories are the ONLY way a Clock or a TimeZone enters the injected graph; see
    // issue #7 for the small set of Compose-side reads that bypass it by design.
    factory { kotlinx.datetime.TimeZone.currentSystemDefault() }
    factory<kotlin.time.Clock> { kotlin.time.Clock.System }
}
