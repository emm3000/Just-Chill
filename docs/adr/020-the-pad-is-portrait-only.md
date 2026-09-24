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
- **The pad is one column** in `AddTransactionScreenContent`, which picks
  between two arrangements by height alone: `StackedTall`, with the combos
  wrapped under their eyebrow, and `StackedShort`, with the combos in one row
  that scrolls sideways, 48dp keys and 4dp gaps.
  Both behave exactly as ADR 019 set them.
- **`StackedTall` whenever it fits** (amended by #425).
  `PadArrangementLayout` measures the tall pad without its hero and keeps it
  when that height plus the hero's floor, 1.3 × `amountHero.fontSize` (IBM
  Plex Mono's line box), fits in the window; otherwise `StackedShort`. The
  rule reads the current combos, width and font scale, so no height token
  stands in for it. It replaced a fixed 728dp threshold that a Redmi 15C
  (360x800dp) missed with the 3-button bar: the emulator configured as that
  phone measures a 24dp status bar and a 48dp bar, exactly 728dp left, and
  the phone itself loses more than that. With three combos at 360dp the tall
  pad is 576dp at font 1.0, so it holds from 659.2dp.
- **Lint.** `LockedOrientationActivity` and `NonResizeableActivity` are
  ignored in `androidApp/lint.xml`, and `DiscouragedApi` is ignored for the
  manifest only, each for this reason.

### What was deleted

Everything that served only the wide windows: the `SideBySide` and
`SideBySideNarrow` arrangements, the keypad pane width and its 236dp floor,
`CapturePadLayout` with its slots and the `movableContentOf` that carried the
pad across the Row/Column switch, `PadForm`'s stacked chips, `FormMetaRow`'s
compact spacing and the note action's 48dp minimum width,
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
