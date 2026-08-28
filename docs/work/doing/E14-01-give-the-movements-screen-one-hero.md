# E14-01 — Give the movements screen one hero

**Epic:** [E14 — Information hierarchy](../epics/E14-information-hierarchy.md)

## Done when

- [x] `SeeTransactionsHeader` renders the browsed month as the title in `headlineL` with the
      prev/next chevrons inline; the "Transacciones" title and the `PlainMonthSelector` row are gone
      from `SeeTransactionsScreen`.
- [x] `SeeTransactionsMonthSummary` renders one hero — `MonthSummaryUi.spend` through `AmountHero`
      under the eyebrow "Gastaste este mes" — with income and net on one `textSecondary` line
      below; the three-column strip is gone.
- [x] A "Hoy no anotaste nada" card renders only when the browsed month is `currentMonth` and no
      `DayGroup` is dated today; `SeeTransactionsUiStateTest` pins both the shown and the hidden
      case. Tapping it opens `AddTransactionRoute`.
- [x] `TransactionUi` carries `categoryName` and `accountName`; `TransactionRow` titles with
      `description`, falling back to `categoryName`, and subtitles with the account (prefixed by the
      category when the title is the description). A host test pins the fallback. The literal
      "Sin descripción" and `readableTime` no longer exist in `:ui-android` or `:presentation`.
- [x] `PendingRecurringRow` carries an `IconTile` so its text column aligns with `TransactionRow`.
- [x] `HhBottomBar` labels read "Movimientos" and "Anotar" where they read "Ver" and "Agregar".
- [x] Every `@Preview` in `SeeTransactionsScreen.kt` and `PendingRecurringComponents.kt` still
      compiles and shows the new layout.
- [ ] `./gradlew qualityGate --rerun-tasks` and `assembleDevDebug` green; an emulator screenshot
      matches the canvas' "Inicio · propuesta" artboard.

## Context

"Movimientos", not "Inicio": E06 retired a Home tab under that name. Category and account names
are not in `TransactionUi` today — source them in the `:presentation` mapper, never in a composable.

## Status

Everything but the screenshot is proved and shipped; the ticket stays open for that one check.
Reviewer round 1 (FIX-FIRST on c0ae0562) is answered in full.

- Proofs: `TransactionUiTest` (title/subtitle fallbacks), `SeeTransactionsUiStateTest`
  (`isTodayNudgeVisible`: shown, plus hidden on a dated-today group, a past month, an active filter,
  an empty ledger and a clock that has not spoken), `SeeTransactionsViewModelTest`
  (`today` lands even when the pending flow never emits), `:ui-android:compileDebugKotlin` for the
  previews. `qualityGate --rerun-tasks` and `assembleDevDebug` green.

- **Open:** the emulator screenshot against the "Inicio · propuesta" artboard. Four deliberate
  divergences to judge there:
  - Header chevrons and actions are 48dp, not the artboard's 32/40px — DESIGN_SYSTEM §4 makes 48
    non-negotiable. The outer padding gives back the 14dp a 48dp target wraps its 20dp glyph in, so
    the icons still stand on the 24dp column the rows use.
  - `TransactionRow`'s tile went `Sm` → `Lg`: the artboard's 40px, and the only size that makes the
    pending row's `Lg` tile line up with it.
  - No rule under the summary block: the artboard draws none, and it would double the nudge card's
    own border.
  - The month title steps down from 28sp when it must. Measured against the bundled Inter faces:
    "Septiembre 2026" wants 230dp; the title gets 228dp at 448dp wide (27sp), 191dp at 411 (23sp),
    173dp at 393 (21sp). Below ~370dp it reaches the 18sp floor and ellipsises — the floor stays a
    real type token rather than shrinking the screen title under the row titles.
