# :feature:loan — CLAUDE.md

The informal-loan ledger: the person list, a person's loans, a loan's detail with its payments, and the add/edit loan form. ViewModels, `UiState` / `Intent` / `Effect`, Compose screens, routes and `loanEntries` all live here, in `com.emm.justchill.feature.loan`.

`id("justchill.android.feature")` plus `kotlinx-coroutines-core`, `kotlinx-datetime`, `androidx-lifecycle-runtime-compose` and `androidx-material-icons-extended`. Depends on `:core:ui` and `:core:domain` and nothing else; `checkModuleBoundaries` fails the gate on any other edge.

`./gradlew :feature:loan:testDebugUnitTest`. The MockK ViewModel suite moved here from `:androidApp` with the ViewModels; `MainDispatcherRule` and `FakeTodayFlow` come from `:core:testing`.

## Composition tests

This module is the repo's composition-test pilot. Robolectric and `androidx.compose.ui:ui-test-junit4` are `testImplementation` here alone, with `ui-test-manifest` on `debugImplementation` for the host activity, and `testOptions.unitTests.isIncludeAndroidResources` is enabled in this module's build file rather than in `AndroidLibraryConventionPlugin`, so no other library module pays for it.

`LoanDetailScreenTest` renders `LoanDetailScreen` inside `EmmTheme` with a recording `onIntent` and pins what the ViewModel suite cannot see: both top-bar actions exist by content description and carry a click action, the `ABONOS` eyebrow renders, the `Registrar abono` CTA renders and clicks through to `OnAddPaymentClick`, and each top-bar click records `OnEditLoanClick` / `OnDeleteLoanClick`. A settled summary (`remainingCents = 0L`) asserts the CTA **keeps** its click action and is not enabled: `StickyCTA` passes `enabled = interactive` to one `clickable`, and `clickable(enabled = false)` leaves `SemanticsActions.OnClick` in place and adds `disabled()`, so the disabled state is a disabled semantic, never a missing node (#243).

`src/test/resources/robolectric.properties` carries `sdk=35` and `qualifiers=w411dp-h891dp` once for the module, not a `@Config` per suite: Robolectric refuses SDK 36 on Java 17 (`Android SDK 36 requires Java 21`) and this project is on the 17 toolchain, and the fixed viewport keeps the eyebrow and the CTA composed regardless of the host's default device. A new suite here inherits both and adds no annotation.

Copying this into another module: take the three dependency lines, the properties file, and nothing else. `debugImplementation(ui-test-manifest)` merges a test-only `ComponentActivity` into that module's debug manifest — AndroidX's documented way to host `createComposeRule()`, debug-only, and it stays. `LoanDetailScreenTest` sits at exactly 8 functions, the per-file ceiling in `.claude/rules/kotlin-style.md`, so the next screen suite in this module is a new file rather than more tests in this one.

## Koin and the graph

`loanModule` is declared here and binds the four ViewModels, nothing else. `:androidApp`'s `wiring/LoanWiring.kt` includes it and adds the five loan use cases, and is the only file outside this module that binds anything of the ledger's; the repositories stay in `:androidApp`'s `core/di/DataModule.kt`, since only the app sees `:core:database`. Every ViewModel here is listed in `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`.

## Dates

`TodayFlow` decides the day, the injected `Clock` only ever supplies the time of day. Both payment sheets open on `TodayFlow`'s day, a new loan is lent on `TodayFlow`'s day at the clock's hour, and editing a payment composes the newly picked date with the payment's **original** time of day. Writing the clock's day instead is the regression these tests exist for.

## Interest

Stored in basis points. `percentTextToBps` deliberately does not clamp: `MAX_INTEREST_BPS` in `:core:domain` is the one place an out-of-range rate is rejected, so a clamp here would silently accept what the domain means to refuse. `sanitizeInterestPercentInput` drops exactly the keystrokes the parser would discard — decimals past two, separators past the first — so the field never shows text it does not parse.

## Payments

- The sheet's amount ceiling is the loan's remaining; while editing a payment it is remaining **plus** that payment's own amount, because the edit replaces it rather than adding to it.
- Every confirm intent is idempotent while in flight. `OnDeleteLoanConfirm`, `OnDeletePaymentConfirm`, `OnPaymentConfirm` and `Save` dispatched twice before the first resolves call their use case once.
- A loan that disappears from `byId` emits `LoanDeleted`, never an endless spinner, and a soft delete that re-emits null emits it only once.
- Remaining follows the repository flow after a payment edit or delete; nothing refreshes by hand.

## Tone

Positive remaining takes `success`, a settled balance takes the muted step, and an unsettled non-positive remaining stays monochrome. `LoansSection` on the accounts screen shows the same total and must keep matching. `PersonBalanceUi` and its `toUi` are shared vocabulary in `:core:ui`'s `core/ui/loan/`, not a copy of this module's models.
