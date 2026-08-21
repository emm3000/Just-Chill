# E01-16 — Clear the session after account delete

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] after a successful `delete_account` RPC the local session is cleared unconditionally — a
      failing revoke POST can no longer leave the device Authenticated for a deleted account
- [ ] the revoke failure is logged, not surfaced: `deleteAccount()` stays `Unit`, with no
      `SignOutResult`-shaped result
- [ ] a test covers `deleteAccount()`'s call shape, including revoke-fails-but-session-clears

## Context

The failure is not swallowed today — it escapes through `authCall`, so deletion reports failure for
an account that IS gone while `clearSession()` never runs. `signOut()`'s swallow-then-clear is the
shape to copy; its `SignOutResult` is not, because the caller has no remaining choice to make.
