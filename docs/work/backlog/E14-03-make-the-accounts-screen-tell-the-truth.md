# E14-03 — Make the accounts screen tell the truth

**Epic:** [E14 — Information hierarchy](../epics/E14-information-hierarchy.md)
**Blocked by:** E14-01

## Done when

- [ ] `AccountsScreen`'s header shows the current month's expenses and income under the eyebrows
      "Gastado en <mes>" and "Ingresado"; no figure labelled "Saldo total" remains anywhere in
      `:ui-android`.
- [ ] Each account row shows that account's net for the current month on the right, captioned
      "este mes"; a `ViewModel` host test pins the per-account net against a fixture with two
      accounts.
- [ ] Loans render under their own "Préstamos" eyebrow — one row, the total owed to the user in
      `success` — and no longer inside the accounts list.
- [ ] The "Gestionar categorías" footer is gone from `AccountsScreen`; `CategoriesListRoute` keeps
      its door in `ProfileScreen`.
- [ ] `./gradlew qualityGate --rerun-tasks` and `assembleDevDebug` green; an emulator screenshot
      matches the canvas' "Cuentas · propuesta" artboard.

## Context

Accounts have no opening balance, so the app cannot show a balance — it shows a monthly net and
must say so. Verify that the loans row was not the sole door to `LoansRoute` before moving it
(E06 constraint).
