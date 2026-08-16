# E01-19 — Signal a refused disclosure gate outside Perfil

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] a user who signs into a new account and never opens Perfil gets some signal that backup is
      refused, outside the Perfil row
- [ ] the signal does not itself trigger an upload — the disclosure gate still requires explicit
      acknowledgement

## Context

The disclosure gate refuses every cycle silently today; `BackupRowUi.DisclosurePending` is the only
place it surfaces, and it is invisible if Perfil is never opened.
