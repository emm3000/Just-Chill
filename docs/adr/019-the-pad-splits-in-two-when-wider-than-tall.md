---
status: accepted
date: 2026-09-24
---
# The pad splits in two when the window is wider than tall

Amends ADR 017's pad layout. ADR 017 stacks the pad in one column: menu,
month total, hero, selectors, date and note, combos, keypad, save. That column
needs about 560dp before the hero gets any height, so on a landscape phone
(800x360) or a split-screen half (360x350) the hero collapsed to nothing and
the save button left the window (#414's references). No arrangement of one
column fits a four-row keypad, the hero and the save button in 350dp.

## Decision

`CapturePadLayout` reads its own constraints through `BoxWithConstraints` and
picks one of four arrangements. Nothing is added and no step moves behind a
tap; the save stays one white full-width button under everything.

- **Taller than wide, at least `WrappedCombosMinHeight` (728dp) tall:** one
  column as ADR 017 set it, with the menu beside the month total instead of
  above it. The combos wrap. 728dp is the fixed column (~573dp) plus three
  wrapped combo rows and the hero at `amountHero`.
- **Taller than wide, shorter than that:** the same column, but the combos are
  one row that scrolls sideways without their eyebrow, 4dp above and below
  it, and the keypad keeps 52dp keys with 4dp gaps and no bottom padding.
  That returns 52dp to the hero, which keeps `amountHero` size on a 360x640dp
  phone once the status bar and a three-button bar take 72dp (#418).
- **Wider than tall:** two panes. The menu and hero share the top row and take
  the leftover height; the month total, selectors, date and note and combos
  sit on the left; the keypad sits on the right; the save spans the bottom.
  The keypad pane is half the width but never less than four 48dp keys, three
  4dp gaps and the s4 padding on both sides (236dp), so every key keeps a 48dp
  target. The combos scroll in one row without their eyebrow.
- **Wider than tall, with a left pane under `ReadableFormMinWidth` (280dp):**
  the same two panes, with the account and category chips stacked one per row
  and no month line.

The rule is the window's shape, not a size class: no `material3-adaptive` or
`window-size-class` dependency is added. Both thresholds live in
`:core:ui`'s `theme/EmmBreakpoints.kt`.

### The 360x350 split window

350dp tall: the save block takes 81dp (hairline 1, padding 12 + 16, button 52),
the keypad 4 × 48 + 3 × 4 = 204dp, which leaves 65dp for the menu and hero row.
The left pane holds four 48dp rows (account, category, date and note, combos),
192dp, under the keypad's 204dp. The month total fits in neither pane nor the
top row, which keeps 296dp for a hero that needs about 245dp, so this cell
drops it. The movements screen stays one tap away from the menu.

## Consequences

The hero keeps `amountHero` size in every fontScale-1.0 cell of
`@PreviewWindowEdges`. On a tablet in landscape the hero row is mostly empty
space. At 360x350 the left pane is 124dp and each chip 92dp, which leaves the
label about 20–40dp: the account reads "Betsy", the category ellipsizes to
"G…". The chip stays usable through its 48dp target, its colour dot and the
picker sheet one tap away. A combo past the window's edge needs a swipe
before its tap when the combos scroll.

## Considered options

- **Key height from the space available.** Recovers 16dp at most; the 640dp
  column was short by more than the hero's 64dp.
- **A minimum hero height.** Pushes the save button out of the frame instead
  of fixing the budget.
- **A scrolling pad.** Refused by #415: the save must never be off screen.
- **Stacked below a readable width.** At 360x350 it pushes the save out of the
  frame, which #415 refuses.
- **Dropping `SelectorChip`'s chevron on request.** Changes an atom's API for
  one split-screen cell and still ellipsizes the category.
