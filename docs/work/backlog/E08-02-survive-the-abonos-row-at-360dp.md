# E08-02 — Survive the abonos row at 360 dp

**Epic:** [E08 — design system conformance](../epics/E08-design-system-conformance.md)

The Redmi 15C preview added in E05-15 caught `Pill("Transferencia")` wrapping mid-word in
`LoanPaymentRow`. Two exposed `IconBtn`s take 100 dp of a 360 dp row, leaving the `weight(1f)`
column ~216 dp for content that wants ~228 dp. `Pill`'s `Text` carries no `maxLines`, so it breaks
the word instead of truncating.

## Done when

- [ ] `Pill` never wraps: its `Text` is single-line and truncates. Every one of its eleven consumer
      files still renders correctly in its own previews — `RecurringSelectorPills`, `ComparisonPill`,
      `TodayPill` and `SavingsRateBlock` included.
- [ ] `LoanPaymentRow` shows one overflow affordance instead of two exposed icon buttons, reusing
      the existing `core/ui/atoms/EmmRowMenu.kt`. Editar and Eliminar keep their labels and their
      `contentDescription`s; no action is lost.
- [ ] `EmmRowMenu`'s tappable box is at least 48×48 dp while its `MoreVert` artwork stays 18 dp.
      This takes that one site from E08-01, which keeps its condition and finds it already met.
      `AccountRow` and `RecurringMovementRow` are re-checked — the bigger box shifts their layout too.
- [ ] `LoanPaymentsList.kt`'s previews carry `@PreviewRedmi15C` alongside their bare `@Preview`, and
      no preview in the loan feature shows a wrapped or truncated pill at 360 dp.
- [ ] Verified on the emulator resized to the device (`adb shell wm size 720x1600`,
      `wm density 320`), then reset — the epic requires a device, not a diff.
- [ ] `./gradlew qualityGate` and `./gradlew assembleDevDebug` both pass.
