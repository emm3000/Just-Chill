# :core:testing — CLAUDE.md

JVM test-fixture module. `com.emm.justchill.core.testing`. Depends on `:core:domain` only.

- `MainDispatcherRule` — swaps the main dispatcher for tests touching `viewModelScope`.
- `FakeTodayFlow` — a controllable `TodayFlow`; use in place of `ClockTodayFlow`, whose self-rescheduling `delay` hangs `runTest`.

A feature module gets it through `justchill.android.feature`'s `testImplementation`, never `implementation`. Production code never depends on `:core:testing`; the gate's boundary check allows the edge only from a test source set.
