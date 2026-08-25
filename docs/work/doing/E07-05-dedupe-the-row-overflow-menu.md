# E07-05 — Dedupe the row overflow menu

**Epic:** [E07 — baseline burn-down](../epics/E07-baseline-burndown.md)

## Done when

- [ ] `EmmRowMenu(contentDescription, onEdit, onDelete)` lives in `core/ui/atoms/EmmRowMenu.kt` and
      serves both rows — `AccountRowMenu` (`hh/account/AccountRow.kt`), `RecurringRowMenu`
      (`hh/recurring/RecurringMovementRow.kt`) and both copies of `MenuIcon` are gone
- [ ] the caller supplies the `contentDescription`: the account row says "Opciones de cuenta" and the
      recurring row says "Opciones" — that difference is a real accessibility label, not drift to
      normalise away
- [ ] no other Spanish copy changes, and both rows render identically on a device
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

Surfaced by E07-04's split, which relocated both menus but was forbidden from merging them: a
baseline burn-down may only shrink a baseline, and this is not a baseline entry.

Measured with `diff`: each menu taken with its private `MenuIcon` is 89 lines, and the two differ in
exactly two — the function name and the `contentDescription`. What repeats is style tokens, not
business logic, which is why the merged component is an atom and not a feature helper.
