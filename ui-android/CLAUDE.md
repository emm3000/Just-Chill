# :ui-android — CLAUDE.md

Android's Compose UI module: screens, navigation, theme and widgets. It renders what
`:presentation` exposes and owns nothing else — no ViewModels, no DI, no formatters.

Plain `com.android.library` (ADR 011), root package `com.emm.justchill.{hh.<feature>, core,
components}`, `minSdk = 28`. Depends on `:presentation` (api), `:domain`, `:data`. A feature owns
`hh/<feature>/` — its Screens plus `<Feature>Entries.kt` for nav wiring. Cross-feature: `hh/shared/`
(nav host, bottom bar, atoms), `core/theme/` (the design tokens, `docs/DESIGN_SYSTEM.md` holds the
criteria), `components/` (legacy `Emm*` widgets, not the design system).

No Koin module here, and no exception: a new feature registers its module in `:presentation`'s
`appModules()`. Routes, doors and the back stack: `.claude/rules/navigation.md`, auto-loaded on any
read under `hh/shared/` or of an `*Entries.kt`.

## Navigation shell

- The bottom bar holds four tabs plus the centre add button, no more. `HhBottomBar`'s 10sp labels
  already sit under the 4.5:1 AA floor; a fifth tab shrinks them further. A new destination swaps a
  tab out, never appends one.
- A route promoted to a tab changes supertype to `BottomBarRoute` and loses its back affordance: a
  leftover back button on a tab pops the start destination (`ReportTopBar` once took an `onBack`).
- A shell screen never recomputes a number another screen owns. `ReportScreen` owns income, spend
  and balance; `SeeTransactionsScreen` owns the transaction list. Before adding a section, find the
  owner.
- `danger` on a pending recurring row's overdue period label (`PendingRecurringComponents.kt`,
  `ConfirmRecurringSheet.kt`) and on `SavingsRateBlock`'s deficit rate is deliberate: both are broken
  states, not amounts. The trend pill beside the rate is a comparison and stays `PillTone.Neutral`.
- `hh/shared/PlatformHostActions.kt`'s flags (`supportsBackup`, `showGoogleSignIn`, …) are plain
  constants the nav entries read; collapsing that seam is a separate change.

## UI conventions

- Every Compose artifact is governed by `androidx-compose-bom` — never add a `version.ref` to a
  Compose library. `koin-compose` pulls a few `org.jetbrains.compose.*` shims whose only dependency
  is the matching `androidx.compose.*` artifact; swapping to `koin-androidx-compose` would touch
  every injection site.
- Resources are ordinary Android resources under `src/main/res/`, reached through
  `com.emm.justchill.shared.R`.
- Screens are callback-driven composables taking `state` + `onIntent`; `SnackbarHostState` lives in
  the root `Scaffold` of `AppNavHost.kt`. Semantic ids (`iconId`/`colorId`) resolve at render time
  via `CategoryResolve.kt`, never in a mapper. `compose_stability.conf` declares `:presentation`'s
  state classes and `:domain` values stable; if a list stutters, read the compiler metrics first.
- User-facing strings are Spanish, from `:presentation`'s `UiStrings`; errors reach the user
  through `DomainException.toUserMessage()`, never a raw message.
- A screen never applies window insets itself: `AppNavHost`'s `Scaffold` owns them
  (`contentWindowInsets = WindowInsets.safeDrawing`) and hands them down as `PaddingValues`, so
  `statusBarsPadding`/`navigationBarsPadding`/`imePadding` double-pad. `ModalBottomSheet` is its
  own window and the one exception.

## Testing

`./gradlew :ui-android:testDebugUnitTest`. Pure UI logic gets a plain function next to the screen
and a test here (`commitHashUi()` is the pattern). There is no Compose UI test harness. The Koin
graph test and the formatter/mapper suites belong to `:presentation`.
