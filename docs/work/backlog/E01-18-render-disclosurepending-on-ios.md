# E01-18 — Render DisclosurePending on iOS

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] iOS's backup row view switches on `BackupRowUi.DisclosurePending` and renders its own state,
      not a silently skipped case
- [ ] the rendered state matches `BackupRowUi.severity()` (`Warning`, never `Danger`)

## Context

SKIE exports `BackupRowUi.DisclosurePending` to Swift today with no consumer — the case lands with
the Profile+backup screen (`docs/swiftui/PLAN.md` slice S9).
