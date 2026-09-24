---
status: accepted
date: 2026-09-24
---
# The pad is portrait-only

ADR 019 split the capture pad into two panes whenever the window was wider
than tall, with a narrower variant for split-screen halves. Two tickets (#415,
#418) and a third (#421) went into windows nobody uses. The pad is a
one-handed keypad that records a movement in under fifteen seconds; neither
the PRD nor ADR 017 asks for landscape, tablets or multi-window, and the one
device it runs on is a phone held upright.

## Decision

- **`MainActivity` locks portrait** with `android:screenOrientation="portrait"`.
- **`MainActivity` opts out of multi-window** with
  `android:resizeableActivity="false"`. On a phone that also turns off
  split-screen, freeform and desktop windowing.
- **`CapturePadLayout` keeps one column** and picks between two arrangements
  by height alone: `StackedTall` from `WrappedCombosMinHeight` (728dp) up,
  with the combos wrapped under their eyebrow, and `StackedShort` below it,
  with the combos in one row that scrolls sideways, 48dp keys and 4dp gaps.
  Both behave exactly as ADR 019 set them.
- **Lint.** `LockedOrientationActivity` and `NonResizeableActivity` are
  ignored in `androidApp/lint.xml`, and `DiscouragedApi` is ignored for the
  manifest only, each for this reason.

### What was deleted

Everything that served only the wide windows: the `SideBySide` and
`SideBySideNarrow` arrangements, the keypad pane width and its 236dp floor,
the `movableContentOf` slots that carried the pad across the Row/Column
switch, `PadForm`'s stacked chips, `FormMetaRow`'s compact spacing,
`ReadableFormMinWidth`, the `800x360 landscape` and `360x350 split` preview
cells, `@PreviewScreenSizes` (its landscape, tablet, foldable and desktop
cells; its `Phone` cell is declared again with the same values) and the
side-by-side hero test. The `keyHeight` override and the scrolling combos
row stay: `StackedShort` uses both.

## Consequences

Phones honour both attributes. From Android 16 (targetSdk 36) the system
ignores orientation and resizability restrictions on displays of 600dp or
more, so on a tablet or an unfolded foldable the stacked pad renders wide.
No screenshot cell covers that and none will; the gap is accepted. The PRD
has no orientation or window-size row, so this ADR is the only record of the
decision. A landscape or split-screen layout comes back only with a PRD row
that asks for it.

This supersedes ADR 019.
