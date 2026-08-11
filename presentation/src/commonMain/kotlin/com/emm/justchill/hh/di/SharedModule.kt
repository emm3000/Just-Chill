package com.emm.justchill.hh.di

import com.emm.domain.shared.UniqueIdProvider
import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import org.koin.dsl.bind
import org.koin.dsl.module

// Cross-cutting agnostic/domain wiring shared across features.
// DefaultUniqueIdProvider lives in ui-android commonMain (slice 8a), so this
// binding belongs in commonMain.
val sharedModule = module {
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    // Constructor-DSL deps (Koin does not use Kotlin default params): the TimeZone is what the
    // two transaction write paths and the frequency windows use to ask what today is locally —
    // the one question about a date that still needs a zone. The Clock reaches HomeViewModel.
    // FQN avoids an ImportOrdering detekt violation (mirrors HomeViewModel).
    factory { kotlinx.datetime.TimeZone.currentSystemDefault() }
    factory<kotlin.time.Clock> { kotlin.time.Clock.System }
}
