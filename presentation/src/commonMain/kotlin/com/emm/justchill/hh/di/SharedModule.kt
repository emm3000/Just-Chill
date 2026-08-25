package com.emm.justchill.hh.di

import com.emm.domain.shared.UniqueIdProvider
import com.emm.justchill.core.time.ClockTodayFlow
import com.emm.justchill.core.time.TodayFlow
import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

// Cross-cutting agnostic/domain wiring shared across features.
// DefaultUniqueIdProvider lives in ui-android commonMain (slice 8a), so this
// binding belongs in commonMain.
val sharedModule = module {
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    // Depends on the two factories below, so it lives beside them: any ViewModel that needs a
    // live "today" gets it from here instead of re-deriving one from a one-shot Clock read.
    factoryOf(::ClockTodayFlow) { bind<TodayFlow>() }

    // The composition root is where reading the machine belongs, and these two factories are
    // the ONLY way a Clock or a TimeZone enters the injected graph: no use case and no
    // ViewModel defaults either one any more, so every
    // "what day/month is it" in :domain and :presentation is answered from here. Koin does not
    // apply Kotlin default params, so that was already true in production — deleting the defaults
    // is what makes it true for a hand-written caller too. :ui-android has four production-code
    // LINES (three deliberate reads; DatePickerSheet accounts for two lines) that bypass the graph
    // by design. #7 names every one of them and says which are deliberate; do not infer from this
    // comment that the rest of the app never reads the machine.
    // FQN avoids an ImportOrdering detekt violation.
    factory { kotlinx.datetime.TimeZone.currentSystemDefault() }
    factory<kotlin.time.Clock> { kotlin.time.Clock.System }
}
