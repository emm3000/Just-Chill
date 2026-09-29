# :feature:loan — CLAUDE.md

The informal-loan ledger: the person list, a person's loans, a loan's detail with its payments, and the add/edit loan form. One package, `com.emm.justchill.feature.loan`, split by source set (ADR 024 Decision 5, #488): `commonMain` holds the four ViewModels, their `UiState` / `Intent` / `Effect`, `loanModule` and the Compose-free models they build (`LoanRowUi`, `LoanPaymentRowUi`, `LoanSummaryUi`, `LoanPaymentFormUi`, `PaymentMethodUi`, `InterestPercent`, the `LoanFormSheet` and `PaymentSheet` enums); `androidMain` holds the screens, sheets, dialogs, the `@Serializable` routes, `loanRoutes` and `loanEntries`. Nothing in `commonMain` imports Compose, Android, Navigation 3 or kotlinx-serialization.

`justchill.kmp.feature` and `justchill.kmp.robolectric`, plus `kotlinx-coroutines-core` and `kotlinx-datetime` on `commonMain` and `androidx-lifecycle-runtime-compose` and `androidx-material-icons-extended` on `androidMain`. Depends on `:core:ui`, `:core:presentation` and `:core:domain` and nothing else; `checkModuleBoundaries` fails the gate on any other edge.

`./gradlew :feature:loan:testAndroidHostTest`; `./gradlew test` does not reach it, the gate does. The MockK ViewModel suite moved here from `:androidApp` with the ViewModels; `MainDispatcherRule` and `FakeTodayFlow` come from `:core:testing`.

## Composition tests

This module is the repo's composition-test pilot; `:feature:onboarding` (#240), `:feature:category` (#272), `:feature:report` (#271) and `:feature:transaction` carry the harness too. In a `justchill.kmp.feature` module the harness is the `justchill.kmp.robolectric` opt-in (#512), applied after `justchill.kmp.feature` and only in the modules that run composition tests: `:feature:account`, `:feature:auth` and `:feature:profile` do not apply it. It puts Robolectric, `androidx.compose.ui:ui-test-junit4` and `ui-test-manifest` on `androidHostTestImplementation`, since a KMP library has no build types, so there is no `debugImplementation`, and the host-test runtime is where the manifest's `ComponentActivity` must land. It sets `isIncludeAndroidResources` on the host-test compilation `KmpLibraryConventionPlugin` already enabled, never through a second `withHostTest {}`, which throws `Android host tests have already been enabled`. Without the flag the Robolectric suites fail. A module still on `justchill.android.feature` uses `testImplementation`, `debugImplementation(ui-test-manifest)` and `testOptions.unitTests.isIncludeAndroidResources` instead.

`LoanDetailScreenTest` renders `LoanDetailScreen` inside `EmmTheme` with a recording `onIntent` and pins what the ViewModel suite cannot see: both top-bar actions exist by content description and carry a click action, the `ABONOS` eyebrow renders, the `Registrar abono` CTA renders and clicks through to `OnAddPaymentClick`, and each top-bar click records `OnEditLoanClick` / `OnDeleteLoanClick`. A settled summary (`remainingCents = 0L`) asserts the CTA **keeps** its click action and is not enabled: `StickyCTA` passes `enabled = interactive` to one `clickable`, and `clickable(enabled = false)` leaves `SemanticsActions.OnClick` in place and adds `disabled()`, so the disabled state is a disabled semantic, never a missing node (#243).

`src/androidHostTest/resources/robolectric.properties` carries `sdk=35` and `qualifiers=w411dp-h891dp` once for the module, not a `@Config` per suite: Robolectric refuses SDK 36 on Java 17 (`Android SDK 36 requires Java 21`) and this project is on the 17 toolchain, and the fixed viewport keeps the eyebrow and the CTA composed regardless of the host's default device. A new suite here inherits both and adds no annotation.

New suites import `androidx.compose.ui.test.junit4.v2.createComposeRule`, never the deprecated `androidx.compose.ui.test.junit4.createComposeRule`: v2 composes on a `StandardTestDispatcher`, so work is queued and a test that needs it settled calls `waitForIdle()` or advances `mainClock`.

Adding the harness to another KMP module: apply `justchill.kmp.robolectric`, copy the properties file, and nothing else. Check the suite really runs: its `testAndroidHostTest` XML shows the same count as before and 0 skipped. `LoanDetailScreenTest` sits at exactly 8 functions, the per-file ceiling in `.claude/rules/kotlin-style.md`, so the next screen suite in this module is a new file rather than more tests in this one.

### Composing a nav entry against a real back stack

A `<feature>Entries` body needs no `NavDisplay`. `entryProvider { }` is a plain builder returning `(NavKey) -> NavEntry<NavKey>`, `NavEntry.Content()` is public, and `NavBackStack` has a public `vararg` constructor, so the real entry composes against real stack state:

```kotlin
val backStack: NavBackStack<NavKey> = NavBackStack(HomeRoute, DetailRoute, ManifestoRoute())
val resolveEntry: (NavKey) -> NavEntry<NavKey> = entryProvider { onboardingEntries(bindings, onFirstLaunchSeen) }
composeRule.setContent { EmmTheme { resolveEntry(backStack.last()).Content() } }
```

The assertion is then `backStack.toList()` after the click, so seed enough entries that each navigation verb leaves a different shape — a stack where `pop()` and `replaceAll(root)` both land on `[root]` cannot tell the two branches apart. `:feature:onboarding`'s `OnboardingEntriesTest` is the worked example.

The caveat: outside a `NavDisplay`, `LocalLifecycleOwner` is the Robolectric host activity, which is always RESUMED, so `AppNavigator.kt:88`'s mid-transition guard is inert under this recipe and only `push`'s duplicate check survives. Harmless for the onboarding tests, which assert the verbs and not the guard, but a suite meant to pin that guard needs a real `NavDisplay` instead.

## Koin and the graph

`loanModule` is declared here and binds the four ViewModels, nothing else. `:shared`'s `wiring/LoanWiring.kt` includes it and adds the five loan use cases, and is the only file outside this module that binds anything of the ledger's; the repositories are bound in `:shared`'s `core/di/DataModule.kt`, the one module that sees `:core:database`. Every ViewModel here is listed in `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`.

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

Positive remaining takes `success`, a settled balance takes the muted step, and an unsettled non-positive remaining stays monochrome. `LoansSection` on the accounts screen shows the same total and must keep matching. `PersonBalanceUi` and its `toUi` are shared vocabulary in `:core:presentation`'s `core/presentation/loan/`, not a copy of this module's models.
