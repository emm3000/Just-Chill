# E01-05 — Delete the sync engine

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocked by:** E01-02, E01-03, E01-04

## Done when

- [ ] the 9 files in `data/sync/`, `ConflictResolver`, `SyncCursorStore`, `SyncDataUseCase`, `SyncRepository`, and the un-gated `ClaimLocalDataOnAuthenticationUseCase` observer are deleted from `:data`/`:domain`
- [ ] `SyncOrchestrator`, `SyncController`, `SyncStatus`, `SyncEvent`, `DefaultSyncCursorStore`, the cursor keys in `AppPreferences`, and the sync surface of `ProfileViewModel` are deleted from `:presentation`
- [ ] `hh/shared/SyncEventsHandler.kt` and the sync references in `AppNavHost`/`AppNavigator` are deleted from `:ui-android`, and the Android build still passes
- [ ] `presentation/build.gradle.kts`'s `export(project(":data"))` no longer carries the deleted engine into `JustChillKit`'s public ABI

## Context

These three module deletions ship together on purpose — miss one and the Android build breaks,
since `:ui-android` collects what `:presentation` would otherwise stop emitting.
