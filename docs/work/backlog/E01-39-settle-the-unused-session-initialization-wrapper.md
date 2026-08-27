# E01-39 — Settle the unused session-initialization wrapper

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `rg 'awaitSessionInitialization|ObserveSessionUseCase' -g '*.kt'` shows either a production
      caller or none of the three symbols.
- [ ] If they go: `AuthRepository.awaitSessionInitialization`, its `DefaultAuthRepository` override
      and `ObserveSessionUseCase.awaitInitialization` are all deleted, and no test is left asserting
      a signature nothing calls.
- [ ] If they stay: `SupabaseBackupObjectStore` reaches initialization through the domain interface
      instead of `client.auth.awaitInitialization()`, and a test fails when that call is inlined
      back.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.

## Context

Three symbols spanning `:domain` and `:data` have zero production callers; `SupabaseBackupObjectStore`
calls Supabase's `client.auth.awaitInitialization()` raw and skips them. Either the abstraction earns
its keep or it does not — do not leave both.
