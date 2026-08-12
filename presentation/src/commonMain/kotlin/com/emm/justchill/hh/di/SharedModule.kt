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

    // The composition root is where reading the machine belongs, and since the sweep of
    // docs/DATE_AUDIT.md #7 these two factories are the ONLY way a Clock or a TimeZone enters the
    // injected graph: no use case and no ViewModel defaults either one any more, so every
    // "what day/month is it" in :domain and :presentation is answered from here. Koin does not
    // apply Kotlin default params, so that was already true in production — deleting the defaults
    // is what makes it true for a hand-written caller too. One production read in this module still
    // bypasses the graph — SyncOrchestrator's lastSyncedAt stamp — and :ui-android has four more by
    // design. #7 names every one of them and says which are deliberate; do not infer from this
    // comment that the rest of the app never reads the machine.
    // FQN avoids an ImportOrdering detekt violation (mirrors HomeViewModel).
    factory { kotlinx.datetime.TimeZone.currentSystemDefault() }
    factory<kotlin.time.Clock> { kotlin.time.Clock.System }
}
