# :presentation — CLAUDE.md

The compose-free presentation layer: MVI core, every feature's ViewModel/UiState/Intent/Effect, the
Koin DI modules, formatters, `UiStrings` and the preferences ports. `:ui-android` sits on it as a
Gradle dependency. The compose-free rule, the leak grep, ViewModel purity, the once-only Koin
binding and the explicit-import convention: `.claude/rules/presentation.md`, auto-loaded on any read
here.

Plain `com.android.library` (ADR 011): one `src/main`, one `src/test`, `minSdk = 28`. Root packages
`com.emm.justchill.{core, hh.<feature>}`, Android namespace `com.emm.presentation`. `core/` holds
`AppGraph` (`appModules`/`bootstrapAppGraph`), `DispatchersProvider`, `CommitHash.kt` (its KDoc owns
the rationale) and one directory per cross-cutting concern. A feature owns `hh/<feature>/`
(ViewModel + UiState + Intent + Effect + `toUi` mappers) and one Koin module in `hh/di/`; pure
helpers and `UiStrings` sit in `hh/shared/`. The base class and its effect channel:
`docs/work/epics/E12-mvi-core.md`.

## ViewModel state

- A UiState stores what the user chose (an id), never what was resolved; the resolved object is a
  getter over the catalog held in the same state. A stored resolved object is a cache with no
  invalidation, and it is how a movement gets filed under a deleted category with no error.
- A save writes the resolved selection, never the raw id: what reaches the database is what the
  catalog can still resolve.
- An id-based selection has no expiry: a preselected id missing when the catalog first loads still
  lands when a later emission carries it.
- Filter at read time over clearing at write time: a list derived per `transactionType` cannot go
  stale; a list a reducer must remember to clear always can.
- A getter whose deletion leaves the suite green is not covered. Prove coverage by deleting the
  lookup, tier by tier, and watching it go red.
- detekt cannot see this class of defect: `LongMethod` skips `init` blocks, `CognitiveComplexMethod`
  is off, and no `onIntent` nears `CyclomaticComplexMethod`'s 14. A green gate is evidence about the
  Composables, never about the state behind them.
- `AuthViewModel` is the reference shape: the re-entrancy guard lives in the sealed state and
  `launchSubmitting`'s `finally` clears it on success, failure and cancellation alike. Every fix
  lands through `updateState`.
- `TodayFlow.today()` is the one way a ViewModel derives the date; a hand-written `today()` is the
  second way it exists to remove.
- Ver's browsed month follows a midnight rollover only while it equals the month the rollover
  leaves; Report's never moves, a rollover only corrects `isCurrentMonth` and the trends window
  (`SeeTransactionsViewModelTest`, `ReportViewModelTest` pin both).
- `SeeTransactionsViewModel` and `AccountsViewModel` sit at detekt's constructor cap
  (`allowedConstructorParameters: 6`): a datum either needs next joins through the query or an
  existing flow, not a seventh parameter.

## Testing

`./gradlew :presentation:testDebugUnitTest`. `src/test/` holds pure `kotlin.test` suites alongside
MockK, JDBC SQLite and `Dispatchers.setMain`; MockK never leaks into `src/main`.

- `core/AppGraphKoinTest.kt` resolves the whole Koin graph off-device against `TestPlatformModule`;
  a missing binding compiles clean, so this is the only net before a user hits it. It cannot see a
  definition nothing in the graph resolves: `EXPECTED_VIEW_MODELS` and `every single
  bootstrapAppGraph resolves is bound` plug the two ways in, and `CommitHash` (sole consumer
  `koinInject` in `AppNavHost`) is covered by `AndroidPlatformModuleTest` in `:androidApp` instead.
  It also asserts, with a sentinel `Clock` and `TimeZone` bound, that every `Clock`/`TimeZone` field
  on a `com.emm.` class is the bound instance: the net for a `viewModel { }` block forgetting a
  `get()`.
- A ViewModel that injects `TodayFlow` takes a fake: the real `ClockTodayFlow` hangs `runTest`, its
  self-rescheduling `delay` shares the scheduler and `advanceUntilIdle()` never returns.
- A test that waits observes the transition, never samples the state. On a `StateFlow`,
  `first { it }` then `first { !it }` resolves against the current value and passes while proving
  nothing. Subscribe before triggering and assert the recorded sequence; the check is to break the
  production line and watch the test fail naming the missing emission. `rg 'System.nanoTime|Thread.sleep' --glob '*Test.kt'`
  returns nothing; a Ktor `requestTimeout` raced inside `runTest` is wall clock too.
- Every catch-all owes a `CancellationException` arm first. `runCatching` and `catch (e: Exception)`
  both swallow it, the body runs on, and `updateState` (a synchronous CAS) writes after the job is
  dead, so a cancelled loader overwrites the winner; a `StandardTestDispatcher` test very likely
  passes. Rethrow cancellation, then catch `Exception` (`MviViewModel.launchSafe`,
  `BackupFailures.kt`, `DeleteUserAccountUseCase`). Every layer owes the arm, `safeDbCall` and
  `catchAsDomainException` included, and a `Flow.catch` lambda owes it explicitly.

## Gate

On `qualityGate` via the convention plugin: `detektMain` (`detektDebug` + `detektRelease` over
`src/main`), `detektTest`, and `testDebugUnitTest`.
