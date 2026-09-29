# :core:testing — CLAUDE.md

KMP test-fixture module (`justchill.kmp.library`, ADR 024). `com.emm.justchill.core.testing`. Depends on `:core:domain` only.

- `commonMain/`: `FakeTodayFlow` — a controllable `TodayFlow`; use in place of `ClockTodayFlow`, whose self-rescheduling `delay` hangs `runTest`. `FakeBackupAvailability` — a fixed `BackupAvailability`; pass `true` or `false` to pin the snapshot backup door. `NoOpDiagnosticsLogger` — a `DiagnosticsLogger` that drops every warning, for a graph that must resolve one and never asserts on logs.
- `androidMain/`: `MainDispatcherRule` — a JUnit4 `TestWatcher` that swaps the main dispatcher for tests touching `viewModelScope`. JUnit4 is an `androidMain` `api` dependency, so it never reaches `commonMain`.

An Android module gets it through `testImplementation` and resolves its Android variant; a KMP feature through `androidHostTestImplementation` (`justchill.kmp.feature`), never `implementation`. Production code never depends on `:core:testing`; the gate's boundary check allows the edge only from a test source set. It has no tests of its own: `testAndroidHostTest` is on the gate and runs none.
