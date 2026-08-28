# E14-02 — Reshape the add-transaction form

**Epic:** [E14 — Information hierarchy](../epics/E14-information-hierarchy.md)
**Blocked by:** E14-01

## Done when

- [ ] The Ingreso/Gasto segmented control in `TransactionFormControls` carries no `success` or
      `danger` tint on its `+`/`−` marks; selection reads through the text ladder and the surface
      step only (`DESIGN_SYSTEM.md` §1.4).
- [ ] Account and category selectors share one row with the category selector wider; date and note
      are compact inline actions on the row below. With "Supermercado" selected at 390dp no selector
      label truncates.
- [ ] `FrequentComboChip`s render as at most three chips under an `Eyebrow` between the selectors
      and the numpad; the empty band that sat there is gone.
- [ ] The sticky CTA stays the screen's only `accent` element.
- [ ] `./gradlew qualityGate --rerun-tasks` and `assembleDevDebug` green; an emulator screenshot of
      `AddTransactionScreen` with an amount typed matches the canvas' "Anotar · propuesta" artboard.

## Context

E09 forbids adding fields or sheets to this form — this ticket moves what exists and changes no
form behaviour. `AddTransactionScreen` and `EditTransaction` share `TransactionFormControls`; both
must render.
