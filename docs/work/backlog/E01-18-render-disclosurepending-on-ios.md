# E01-18 — Render DisclosurePending on iOS

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocked by:** the Perfil screen not existing yet — see Context.

## Done when

- [ ] iOS's backup row view switches on `BackupRowUi.DisclosurePending` and renders its own state,
      not a silently skipped case
- [ ] the rendered state matches `BackupRowUi.severity()` (`Warning`, never `Danger`)

## Context

SKIE exports `BackupRowUi.DisclosurePending` to Swift today with no consumer — the case lands with
the Profile+backup screen (`docs/swiftui/PLAN.md` slice S9).

That screen does not exist: `iosApp/iosApp/` holds `ContentView.swift` and `iOSApp.swift` and
nothing else. There is no backup row view to add a case to, so this ticket has no artifact to edit
until S9 lands. It is waiting on a file, not on a decision.
