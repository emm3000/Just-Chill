# E05-15 — Preview the loan screens on a Redmi 15C

The author's phone is a Redmi 15C: 720 x 1600 px, ~254 ppi, smallest width 360 dp, so density 2.0
and a 360 x 800 dp viewport — the narrowest config any current screen has to survive. Every loan
preview today is a bare `@Preview`, which renders at the tooling default and proves nothing about
that device.

## Done when

- A multipreview annotation exists in `:ui-android` carrying the device spec exactly once — name it
  for the device, and let `PreviewAnnotationNaming` keep the `Preview` prefix. The spec is
  `width=360dp,height=800dp,dpi=320` with system UI shown.
- Every preview function in the four loan **screen** files — `LoansScreen.kt`,
  `PersonLoansScreen.kt`, `LoanDetailScreen.kt`, `AddEditLoanScreen.kt` — carries that annotation
  **in addition to** its existing bare `@Preview`. No existing annotation, function or fixture is
  removed, renamed or edited.
- The loan component files (dialogs, sheet, summary card, form fields, payments list) are left
  alone: a card in a full phone frame is noise, not a check.
- `config/detekt/baseline-ui-android-main.xml` is unchanged — the new annotation must not push any
  preview out of `UnusedPrivateFunction.ignoreAnnotated` or `TooManyFunctions`.
- `./gradlew qualityGate` and `./gradlew assembleDevDebug` are green.
