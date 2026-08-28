# E12-10 — Wire `EditTransaction`'s "Nueva categoría" button, or delete it

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)

## Done when

- [ ] Tapping "Nueva categoría" inside `EditTransaction`'s `CategoryPickerSheet` either reaches
      `AddCategoryRoute` the way `AddTransactionScreen` does, or the button no longer renders there.
- [ ] `rg 'onAddNew' ui-android/src/main/kotlin/com/emm/justchill/hh/transaction/` shows no lambda
      whose whole body is a dismiss.

## Context

`AddTransactionScreen` takes an `onAddNewCategory` parameter and `TransactionEntries` wires it to a
`nav.push`; `EditTransaction` has no such parameter, so its `CategoryPickerSheet.onAddNew` only
closes the sheet — and `CategoryPickerSheet.onAddNew` is non-nullable, unlike `AccountPickerSheet`'s,
so the button renders regardless. Pre-existing; E12-05 preserved it verbatim while moving the flag
around it. Which way it goes is a product call: the sheet is already open over a form the user is
mid-edit on.
