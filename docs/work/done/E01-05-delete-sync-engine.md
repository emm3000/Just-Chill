# E01-05 — Delete the sync engine

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocked by:** E01-02, E01-03

## Done when

- [x] `:domain`/`:data` — the 9 files in `data/sync/` plus `ConflictResolver`, `SyncCursorStore`, `SyncDataUseCase` and `SyncRepository` are gone; `SyncMutex.kt` and `SyncMutexTest.kt` sit in that same `domain/.../sync/` package and MUST survive (`DeleteUserAccountUseCase` and `BackupOrchestrator` hold it, bound at `BackupModule.kt:28`) — proven: `fd . domain/.../sync` lists only `SyncMutex.kt`
- [x] `DeleteUserAccountUseCase` no longer takes `SyncCursorStore` and its "cursor clear" step is gone — the one E01-06 edit this ticket cannot compile without; `unclaimAll` and the claim machinery (`ClaimLocalDataUseCase`, `ClaimLocalDataRepository`, `DefaultClaimLocalDataRepository`) stay, E01-06 owns them — proven: read `DeleteUserAccountUseCase.kt`, ctor is `authRepository, claimLocalDataRepository, backupMetadataStore, syncMutex, logger`
- [x] `:presentation` — `core/sync/` whole, `hh/di/SyncModule.kt` whole plus its import and `appModules()` entry in `AppGraph.kt`, the cursor keys in `AppPreferences`, the `ClaimLocalDataOnAuthenticationUseCase` observer (`AppGraph.kt:59-61`, `AuthModule.kt:40`) and the class itself, and the sync surface of `ProfileViewModel`/`ProfileUiState`/`ProfileIntent`/`ProfileModule` are gone — proven: `rg 'core/sync|SyncModule|ClaimLocalDataOnAuthenticationUseCase'` returns nothing
- [x] `:ui-android` — `hh/shared/SyncEventsHandler.kt`, `RetryPill.kt` (its only caller is the sync row), the sync surface of `ProfileScreen.kt`, `ProfileEntries.kt:142`, the `SyncController` wiring in `AppNavHost` and the `[SyncEventsHandler]` KDoc line in `AppNavigator.kt:37` are gone; `assembleDevDebug` passes — proven: `rg 'SyncEventsHandler|RetryPill|SyncController'` returns nothing; `qualityGate --rerun-tasks` (which builds the dev variant) is BUILD SUCCESSFUL
- [x] `SYNC_TEMPORARILY_DISABLED` is gone with all 7 call sites (`AppGraph.kt:68`, `ProfileViewModel.kt:223`, `ProfileScreen.kt:199,396,410,421,427`) and the `CyclomaticComplexMethod` suppression at `ProfileScreen.kt:362` that exists only because of them — proven: `rg SYNC_TEMPORARILY_DISABLED` returns nothing
- [x] the 11 sync-engine test files are deleted and the 7 that construct a `SyncController`/`SyncCursorStore` are updated; `SyncMutexTest` and both `BackupOrchestrator` suites are untouched; no detekt baseline carries a dead ID — proven: `rg 'TableSync|SyncRepository|SyncModule|SyncController|RetryPill|SyncEventsHandler|ConflictResolver|SyncCursorStore|SyncDataUseCase|SyncCursorUtils|SyncRowDto' config/detekt/*.xml` returns nothing; `SyncQueriesTest`'s one surviving-query case moved to `data/.../account/GetAccountBalanceQueryTest.kt`
- [x] nothing under `data/src/commonMain/sqldelight/` is deleted — the sync schema stays (epic constraint 2), and so do the now-orphaned `selectPending`/`markSynced`/`insertFromRemote`/`updateFromRemote` queries — proven: `git status --short -- data/src/commonMain/sqldelight/` is empty
- [x] `qualityGate --rerun-tasks` is green and `:presentation:linkDebugFrameworkIosSimulatorArm64` links — `export(project(":data"))` stays at `presentation/build.gradle.kts:40` and nothing in the repo dumps the ABI, so the link is the only proof available — proven: both ran BUILD SUCCESSFUL. The gate reported 241 tasks, not 245 — a re-run reusing the configuration cache skips the four iOS tasks, so the compile for `:presentation` and `:data` and the framework link were forced separately and are green

## Context

E01-04 dropped from `Blocked by`: nothing in ADR 009, the epic or E01-04 itself gates this deletion
on the RLS probe — the archived plan gates the `SNAPSHOT_BACKUP_ENABLED` flip on it, not this.
35 files deleted (~5,281 lines), ~24 edited. The three module deletions ship together — miss one and
the Android build breaks, since `:ui-android` collects what `:presentation` would stop emitting.
The `last_pulled_at_*` / `last_synced_at_*` keys already on installed devices are left in place —
`clearSyncMetadata` and the cursor-clear step are gone, `migrateBuildIdPrefs` copies keys forward
verbatim by type, and the values are inert.
