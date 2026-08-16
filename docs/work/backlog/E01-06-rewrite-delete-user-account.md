# E01-06 — Rewrite DeleteUserAccountUseCase

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocked by:** E01-05

## Done when

- [ ] the "unclaim" and "cursor clear" steps are replaced by: delete the user's Storage objects for that account, then delete the auth user
- [ ] the `delete_account()` RPC call path is otherwise unchanged (`security definer`, `search_path = ''`, revoked from `public`/`anon`)

## Context

`deleteAccount()` still calls bare `client.auth.signOut(SignOutScope.LOCAL)` after the RPC, with
the same defect Phase 0 fixed in `signOut()` — unpinned, and its own open contract question, so
it is not fixed as part of this ticket.
