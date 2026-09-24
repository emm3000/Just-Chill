---
status: accepted
date: 2026-09-24
---
# The pad shares the edit layout

Supersedes ADRs 019 and 020 and amends ADR 017's `±` key. The frequent-combo
chip row was the only block on the capture pad whose height changed, and it
was the only reason the pad had two arrangements, a measured placeholder pad
and a fit rule (#414 to #425). The owner records every movement on the pad and
found that the chips and the `±` key made it worse. The edit screen has
neither and looks the same on every phone.

## Decision

- **One layout for add and edit.** `AddTransactionScreen` draws, top to
  bottom: the menu button and the month total, the `Ingreso`/`Gasto`
  `SignToggle`, the hero at `amountHero` size, the account and category
  chips, `FormMetaRow`, one `weight(1f)` block, the three-key-row `Numpad`,
  `StickyCTA`. Nothing measures the window.
- **No combo chips.** The top-ranked `FrequentCombo` whose account and
  category both still exist silently preselects the pad's account and
  category, so the 15-second capture holds without a chip row. The category
  sheet keeps its frequent-first order (`frequentCategoryIds`).
- **No `±` key.** The pill switches the type in words, and income no longer
  takes a `+` in front of the hero. `Numpad` loses its sign key and its
  key-height and gap overrides; `AmountHero` loses `signed`.
- **Short windows shrink the hero.** Everything except the hero takes a
  fixed 512dp, so the hero and the two gaps around it (at most s8 above and s6
  below) take whatever height is left, and the hero auto-sizes down on
  windows shorter than about 640dp instead of pushing the save button off
  screen.
- **The portrait lock stays** as ADR 020 set it: `MainActivity` is locked
  upright and opted out of multi-window.

## Considered options

- **Keep `±` and drop only the combos.** Rejected: the glyph still has to be
  decoded on every capture, and the pill already says what it does.
- **Move the combos into the category sheet.** Already there, as the
  frequent-first order `frequentCategoryIds` gives the sheet.
- **Keep two arrangements with the combos.** Rejected: all of that code
  exists only because one block's height depends on the data.

## Consequences

`PadArrangement`, `PadArrangementLayout`, the placeholder pad, the combo chip
row, `FrequentComboUi`, `OnFrequentComboSelected` and the tests that pinned
the arrangements are deleted. `GetFrequentCombosUseCase`, the launcher
shortcuts' `comboLabel` and the `FrequentComboChip` atom used by the loan form
stay. On a 360x640dp phone with a three-button bar (568dp left) the hero
renders at about 32sp.
