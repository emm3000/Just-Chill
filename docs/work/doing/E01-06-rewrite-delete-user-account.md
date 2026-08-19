# E01-06 — Rewrite DeleteUserAccountUseCase

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] a `:domain` port deletes every object under one account's Storage prefix, implemented in `:data` over `BackupObjectStore`, which today offers only single-key `delete`. The sweep covers `<uid>/pinned/`, and that needs its own listing pass — `SupabaseBackupObjectStore.list` filters folder entries out and `planPrune` drops nested names, so nothing in the tree can currently see it
- [ ] the sweep runs strictly BEFORE `authRepository.deleteAccount()`, inside the same `syncMutex.withLock`. `delete_account()` runs `delete from auth.users` and `deleteAccount()` clears the local session immediately after, after which `ownedPrefix()` throws `Unauthorized` — so a sweep placed after it cannot resolve a prefix at all. The lock is what stops a concurrent `BackupOrchestrator` upload from repopulating the prefix between sweep and RPC
- [ ] a listing failure aborts before the first delete (prune's rule, epic constraint); individual delete failures are collected and do not stop the run; if ANY object survives the sweep the use case throws and the RPC never runs — the account is destroyed only once its prefix is provably empty
- [ ] that abort carries its own `DomainException`, not a generic one, and the Perfil message names what failed AND that the account was not deleted
- [ ] the "unclaim" step is gone from `DeleteUserAccountUseCase`
- [ ] the `delete_account()` RPC path is otherwise unchanged (`security definer`, `search_path = ''`, revoked from `public`/`anon`). It does not touch `storage.objects` and cannot be taught to — `storage.protect_delete()` refuses direct deletes — so the sweep is necessarily client-side
- [ ] tests pin the three properties nothing covers today: the sweep runs before the RPC, an abort leaves the account alive, and the `pinned/` pass happens

## Context

Deleting Storage after the RPC is impossible, so this reorders the irreversible operation rather
than replacing a step. The asymmetry decides the failure contract: blobs left behind are
unrepairable, since no RLS path reaches a deleted uid's prefix, while a refused deletion is
repaired by retrying. `deleteAccount()` still calls bare `signOut(SignOutScope.LOCAL)` without the
Phase 0 fix — E01-16 owns that. The claim machinery this ticket orphans goes to E01-28.
