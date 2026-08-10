package com.emm.justchill.hh.di

import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.UniqueIdProvider
import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import org.koin.dsl.bind
import org.koin.dsl.module

// Cross-cutting agnostic/domain wiring shared across features.
// DefaultUniqueIdProvider lives in shared-ui commonMain (slice 8a), so this
// binding belongs in commonMain.
val sharedModule = module {
    factory { DateAndTimeCombiner() }
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    // Constructor-DSL deps (Koin does not use Kotlin default params): used by
    // ConfirmRecurringMovementUseCase (TimeZone) and HomeViewModel (Clock).
    // FQN avoids an ImportOrdering detekt violation (mirrors HomeViewModel).
    factory { kotlinx.datetime.TimeZone.currentSystemDefault() }
    factory<kotlin.time.Clock> { kotlin.time.Clock.System }
}
