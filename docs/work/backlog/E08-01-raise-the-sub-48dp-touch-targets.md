# E08-01 — Raise the sub-48dp touch targets

**Epic:** [E08 — design system conformance](../epics/E08-design-system-conformance.md)

## Done when

- [ ] every `clickable` in `:ui-android` sits in a box of at least 48×48dp, or carries one line
      saying why it cannot — `core/ui/atoms/IconBtn.kt` is the pattern: a 48dp box around a 20dp icon
- [ ] the visual size of each icon is unchanged — this grows the touch box, not the artwork
- [ ] rows and sheets whose layout the bigger box shifts are re-checked on a device, not just built
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`docs/DESIGN_SYSTEM.md`: "Touch target minimum 48×48dp — Material's accessibility floor,
non-negotiable." A proximity grep (`clickable` within 8 lines of a `.size()`) named 13 files, from
`SeeTransactionsSearchBar` at 14dp up to several at 44dp, including the `MonthSelector` atom at 36dp.
That grep is a lead, not a measurement: confirm each site reads the box and not an inner icon before
touching it.
