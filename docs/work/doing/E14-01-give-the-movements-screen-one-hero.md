# E14-01 — Give the movements screen one hero

**Epic:** [E14 — Information hierarchy](../epics/E14-information-hierarchy.md)

## Done when

- [ ] `SeeTransactionsHeader` renders the browsed month as the title in `headlineL` with the
      prev/next chevrons inline; the "Transacciones" title and the `PlainMonthSelector` row are gone
      from `SeeTransactionsScreen`.
- [ ] `SeeTransactionsMonthSummary` renders one hero — `MonthSummaryUi.spend` through `AmountHero`
      under the eyebrow "Gastaste este mes" — with income and net on one `textSecondary` line
      below; the three-column strip is gone.
- [ ] A "Hoy no anotaste nada" card renders only when the browsed month is `currentMonth` and no
      `DayGroup` is dated today; `SeeTransactionsUiStateTest` pins both the shown and the hidden
      case. Tapping it opens `AddTransactionRoute`.
- [ ] `TransactionUi` carries `categoryName` and `accountName`; `TransactionRow` titles with
      `description`, falling back to `categoryName`, and subtitles with the account (prefixed by the
      category when the title is the description). A host test pins the fallback. The literal
      "Sin descripción" and `readableTime` no longer exist in `:ui-android` or `:presentation`.
- [ ] `PendingRecurringRow` carries an `IconTile` so its text column aligns with `TransactionRow`.
- [ ] `HhBottomBar` labels read "Movimientos" and "Anotar" where they read "Ver" and "Agregar".
- [ ] Every `@Preview` in `SeeTransactionsScreen.kt` and `PendingRecurringComponents.kt` still
      compiles and shows the new layout.
- [ ] `./gradlew qualityGate --rerun-tasks` and `assembleDevDebug` green; an emulator screenshot
      matches the canvas' "Inicio · propuesta" artboard.

## Context

"Movimientos", not "Inicio": E06 retired a Home tab under that name. Category and account names
are not in `TransactionUi` today — source them in the `:presentation` mapper, never in a composable.
