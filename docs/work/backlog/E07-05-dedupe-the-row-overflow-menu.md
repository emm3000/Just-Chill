# E07-05 — Dedupe the row overflow menu

**Epic:** [E07 — baseline burn-down](../epics/E07-baseline-burndown.md)

## Done when

- [ ] one overflow menu serves both the account row and the recurring-movement row —
      `AccountRowMenu` (`hh/account/AccountRow.kt`) and `RecurringRowMenu`
      (`hh/recurring/RecurringMovementRow.kt`) stop being two copies
- [ ] `MenuIcon` exists once, not once per feature
- [ ] the shared component takes its `contentDescription` from the caller: the account row says
      "Opciones de cuenta" and the recurring row says "Opciones" — that difference is a real
      accessibility label, not drift to normalise away
- [ ] the component lands where `DESIGN_SYSTEM.md` and `CODE_QUALITY.md` say it belongs — the design
      system (`core/ui/atoms/`) or the cross-feature package (`hh/shared/`), decided and stated, not
      a third copy
- [ ] both screens render identically and no Spanish copy changes beyond the two labels above
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

Surfaced by E07-04's split, which relocated both menus but was forbidden from merging them: a
baseline burn-down may only shrink a baseline, and this is not a baseline entry.

The two menus differ in **one line** out of ~45 — the `contentDescription`. `MenuIcon` is
byte-identical. Read `docs/CODE_QUALITY.md` on DRY-over-knowledge before assuming that settles it:
two rows that look alike today can still encode different knowledge. Decide whether an overflow menu
with edit and delete is one idea or two, and say which in the closing commit.
