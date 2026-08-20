# E01-30 — Await the session before the delete RPC

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `deleteAccount` awaits session initialization before the `delete_account` RPC, bounded by a
      timeout, the way `SupabaseBackupObjectStore.ownedPrefix` already does
- [ ] a test pins that a call fired during session restore surfaces as retryable rather than as a
      credentials failure

## Context

`DefaultAuthRepository.deleteAccount` fires `client.postgrest.rpc("delete_account")` with no
`awaitSessionInitialization()`, and it is the only Postgrest call in the class — exactly the case
its own KDoc warns about. Postgrest reads the JWT synchronously from `auth.sessionStatus.value`, so
a call that beats the restore attaches the anon key and comes back 403 under RLS.
