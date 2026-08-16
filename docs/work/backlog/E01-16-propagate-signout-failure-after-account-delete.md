# E01-16 — Propagate the sign-out failure after account delete

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `DefaultAuthRepository.deleteAccount()` no longer leaves a network failure from the trailing
      `client.auth.signOut(SignOutScope.LOCAL)` silently swallowed after a successful RPC
- [ ] a test covers `deleteAccount()`'s call shape — `DefaultAuthRepositorySignOutTest` today
      exercises `signOut()` only

## Context

The right contract is an open question: the account is gone either way, so `signOut()`'s qualified
`SignOutResult.LocalOnly` shape may not fit — decide it here, don't assume it carries over.
