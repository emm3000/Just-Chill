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

- **Wider than tall, the pad has two panes.** `CapturePadLayout` reads its own
  constraints through `BoxWithConstraints`; when `maxWidth > maxHeight`, the
  menu and the hero share the top row and take the leftover height, the month
  total, selectors, date and note and combos sit in the left pane, the keypad
  in the right, and the save button spans the bottom. Nothing is added and no
  step moves behind a tap.
- **Taller than wide, one column, as ADR 017 set it**, with the menu beside
  the month total instead of above it.
- **The combos are one row that scrolls sideways**, on every window: up to five
  wrapping chips cost three rows, more height than a 640dp phone has.
- **In two panes the keys are 48dp with a 4dp gap**, the touch-target floor;
  the combos lose their eyebrow. Everywhere else the keys stay 52dp.

The rule is the window's aspect, not a size class: no `material3-adaptive` or
`window-size-class` dependency is added.

## Consequences

The hero keeps `amountHero` size in every fontScale-1.0 cell of
`@PreviewWindowEdges`. On a tablet in landscape the hero row is mostly empty
space; a maximum keypad width was not needed to keep the save in the frame and
is left out. At 360x350 the selector chips are too narrow to show their labels.
A combo beyond the window's edge needs a swipe before its tap.

## Considered options

- **Key height from the space available.** Recovers 16dp at most; the 640dp
  column was short by more than the hero's 64dp.
- **A minimum hero height.** Pushes the save button out of the frame instead
  of fixing the budget.
- **A scrolling pad.** Refused by #415: the save must never be off screen.
