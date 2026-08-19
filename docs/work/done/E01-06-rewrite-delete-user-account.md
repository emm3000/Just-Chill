# E01-06 — Rewrite DeleteUserAccountUseCase

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] a `:domain` port deletes every object under one account's Storage prefix, implemented in `:data` over `BackupObjectStore` — proven: `domain/.../shared/backup/BackupEraser.kt`, `data/.../backup/DefaultBackupEraser.kt`. The `pinned/` pass the ticket asked for is not written as a second hardcoded prefix: `SupabaseBackupObjectStore.list` dropped every folder pseudo-row (`it.id != null`), so a fixed pair of passes was blind to any OTHER folder and would have reported a clean sweep over one. `ObjectPage` now carries `folders`, and `keysUnder` walks the tree the listing reports — `pinned/` is one of the folders it finds
- [x] the sweep runs strictly BEFORE `authRepository.deleteAccount()`, inside the same `syncMutex.withLock` — proven: `DeleteUserAccountUseCase.kt:31-37`; `DeleteUserAccountUseCaseTest` pins the order with `coVerifyOrder`
- [x] a listing failure aborts before the first delete; individual delete failures are collected and do not stop the run; if ANY object survives the sweep the use case throws and the RPC never runs — proven: `DefaultBackupEraser.keysUnder` completes before `deleteEach`; `refuseIfAnythingSurvives` re-lists the account prefix after the deletes and throws unless the RAW row count is zero. That second listing is what makes "provably empty" true: `bucket().delete(key)` answering 200 does not assert the row is gone, and an object written after the listing was never in it. `DefaultBackupEraserTest` covers a delete that succeeds without removing the row
- [x] that abort carries its own `DomainException`, and the Perfil message names what failed AND that the account was not deleted — proven: `asNotErased` re-types every refusal inside the sweep as `BackupsNotErased` with the original as `cause`; `DomainExceptionExt.kt` renders "No pudimos borrar tus respaldos en la nube. Tu cuenta NO fue eliminada". Without that wrapper a listing failure left as `Unauthorized` and rendered "Credenciales incorrectas o sesión expirada", which says nothing about whether the account died
- [x] the "unclaim" step is gone from `DeleteUserAccountUseCase` — proven: read the file; `unclaimAll` now has zero production callers and E01-28 removes the machinery
- [x] the `delete_account()` RPC path is otherwise unchanged — proven: no `supabase/` file appears in this unit's diff
- [x] tests pin the three properties nothing covered: the sweep runs before the RPC, an abort leaves the account alive, and every folder the listing discovers is walked — proven: `DeleteUserAccountUseCaseTest` (16 tests), `DefaultBackupEraserTest` (13 tests)
- [x] `qualityGate --rerun-tasks` green, and the iOS compile and framework link forced separately — proven: BUILD SUCCESSFUL at 241 tasks, plus `:domain`/`:data`/`:presentation:compileKotlinIosSimulatorArm64` and `:presentation:linkDebugFrameworkIosSimulatorArm64` BUILD SUCCESSFUL. 241 and not 245 for the same reason E01-05 recorded: a re-run reusing the configuration cache skips the four iOS tasks

## Context

Deleting Storage after the RPC is impossible, so this reorders the irreversible operation rather
than replacing a step. The asymmetry decides the failure contract: blobs left behind are
unrepairable, since no RLS path reaches a deleted uid's prefix, while a refused deletion is
repaired by retrying.

The metadata clear moved too, and it is the finding that would have shipped a silent regression.
`BackupMetadataStore.clear` also drops `destinationDisclosedAt`, which `BackupOrchestrator` gates
every upload on — so clearing it before the sweep switched the user's backups off after a deletion
that removed nothing, recoverable only by re-entering Perfil. It sits in the single gap between the
finished sweep and the RPC.

Walk depth is bounded at 5 folders, and exceeding it refuses rather than reporting a prefix clean it
never finished reading. The bound is a guess about a tree nothing currently creates.

`deleteAccount()` still calls bare `signOut(SignOutScope.LOCAL)` without the Phase 0 fix — E01-16
owns that. The claim machinery this ticket orphans goes to E01-28, now unblocked.
