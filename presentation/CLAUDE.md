# :presentation — CLAUDE.md

The compose-free presentation layer for profile, the one feature not yet extracted to `:feature:*` (ADR 015 wave 8 finished transaction, recurring, auth and onboarding; profile waits on #126, then #128 deletes this module): its ViewModel with its `UiState` / `Intent` / `Effect`, the Koin modules (`hh/di/`, listed in `:androidApp`'s `core/AppGraph.kt`), the feature copy, `UiStrings` and the preferences ports. `:feature:{account, category, loan, report, onboarding, recurring, auth, transaction}` already left with their own ViewModels, Koin modules and routes. The MVI base, the shared formatters and the capture models more than one feature reads — `CategoryUi`, `SelectableCategory`, `TransactionUi`, `Catalog` — moved to `:core:ui` (ADR 015), which this module re-exports as `api`. `:ui-android` sits on it as a Gradle dependency. The compose-free rule and its grep, ViewModel purity, the once-only Koin binding and the explicit-import convention: `.claude/rules/architecture.md`.

Plain `com.android.library` (ADR 011): one `src/main`, one `src/test`, `minSdk = 28`. Root packages `com.emm.justchill.{core, hh.<feature>}`, Android namespace `com.emm.presentation`. `core/` holds `DispatchersProvider`, `CommitHash.kt`, `commonCoreModule` and one directory per cross-cutting concern. A feature owns `hh/<feature>/` (ViewModel + UiState + Intent + Effect + `toUi` mappers) and one Koin module in `hh/di/`; pure helpers and `UiStrings` sit in `hh/shared/`. The base class and its effect channel: `:core:ui`'s `mvi/` and `.claude/rules/architecture.md` `## MVI contract`.

## ViewModel state

- An id-based selection has no expiry: a preselected id missing when the catalog first loads still lands when a later emission carries it.
- Filter at read time over clearing at write time: a list derived per `transactionType` cannot go stale; a list a reducer must remember to clear always can.
- A getter whose deletion leaves the suite green is not covered. Prove coverage by deleting the lookup, tier by tier, and watching it go red.
- `AuthViewModel` is the reference shape: the re-entrancy guard lives in the sealed state and `launchSubmitting`'s `finally` clears it on success, failure and cancellation alike. Every fix lands through `updateState`.
- `TodayFlow.today()` (`:core:domain`'s `core/domain/time/`) is the one way a ViewModel derives the date; a hand-written `today()` is the second way it exists to remove.
- Ver's browsed month follows a midnight rollover only while it equals the month the rollover leaves; Report's never moves, a rollover only corrects `isCurrentMonth` and the trends window (`SeeTransactionsViewModelTest`, `ReportViewModelTest` pin both).
- `SeeTransactionsViewModel` and `AccountsViewModel` take six constructor parameters, the ceiling review holds them to: a datum either needs new joins through the query or an existing flow, not a seventh parameter.

## Backup

- `SNAPSHOT_BACKUP_ENABLED` (`core/backup/BackupKillSwitch.kt`) is `false`. Readers: `bootstrapAppGraph`, `BackupDisclosureSignal` and one `if/else` in `BackupSection.kt`, which keeps the sign-in row and the local-only note exclusive; never split it into two reads.
- Flipping it is a disclosure change first: `docs/play/privacy-policy.md`, `docs/play/listing.md` and the Play Data Safety form change in the same release. It is also blocked by the verify phrase: `EmmSnackbar` draws two lines and `toPhrase()` appends table clauses in order, so the last tables' counts get cut off, and ADR 009 makes those counts part of the ship gate.
- Until then no install holds a backup preference key, so renaming one is free. Keys live in `DefaultBackupMetadataStore` over raw `Settings`. The prefixes, the `-1L` "never" value and the `'|'` separator are load-bearing. The streak count and its reason share one key (`count|REASON`) because `Settings` has no transaction. `LocalExportHistory`'s unscoped `last_local_export_at` is the one exception.
- The disclosure check comes first in `takeSnapshot`, before the due check a manual request skips; `uploader.upload` has that one call site. `DestinationUndisclosed` and `OwnerChanged` are refusals: no streak, no reason, no watermark; only a manual `OwnerChanged` emits `BackupEvent.Failed`. Failure state is booked against the captured account and published only while it is still signed in (`publishHealth`); the watermark is rechecked after upload.
- A backup failure never signs out and reaches the UI only as `ProfileEffect.Notify`, never `ShowError`.
- `BackupRowUi` ranks `NeedsAccount` > `DisclosurePending` > `BackingUp`; `DisclosurePending` is `Warning`, never `Danger`. A failure annotates the snapshot (`Failed` carries a `LastSnapshot`); warn on the count, never on the reason. `severity()` and `toMetaText()` stay out of composables (`ProfileViewModelBackupRowTest` pins them). The Perfil badge (`disclosureIsPending`) and `resolveBackupRow` answer the same question and must agree.
- Stale means both more than `BACKUP_STALE_AFTER_DAYS` and a ledger that moved since the last verified snapshot.

## Testing

`./gradlew :presentation:testDebugUnitTest`. `src/test/` holds pure `kotlin.test` suites alongside MockK, JDBC SQLite and `Dispatchers.setMain`.

- `:androidApp`'s `core/AppGraphKoinTest.kt` resolves the whole Koin graph off-device against `TestPlatformModule`; a missing binding compiles clean, so this is the only net before a user hits it. It cannot see a definition nothing in the graph resolves: `EXPECTED_VIEW_MODELS` and `every single bootstrapAppGraph resolves is bound` plug the two ways in, and `CommitHash` (sole consumer `koinInject` in `AppNavHost`) is covered by `AndroidPlatformModuleTest` instead. It also asserts, with a sentinel `Clock` and `TimeZone` bound, that every `Clock` / `TimeZone` field on a `com.emm.` class is the bound instance.
- A ViewModel that injects `TodayFlow` takes `:core:testing`'s `FakeTodayFlow`: the real `ClockTodayFlow` hangs `runTest`, its self-rescheduling `delay` shares the scheduler and `advanceUntilIdle()` never returns.
- A test that waits observes the transition, never samples the state. On a `StateFlow`, `first { it }` then `first { !it }` resolves against the current value and passes while proving nothing. Subscribe before triggering and assert the recorded sequence. `rg 'System.nanoTime|Thread.sleep' --glob '*Test.kt'` returns nothing; a Ktor `requestTimeout` raced inside `runTest` is wall clock too.
