# :ui-android — CLAUDE.md

Android's Compose UI module: screens and navigation. It renders what `:presentation` exposes and owns nothing else: no ViewModels, no DI, no formatters, and since ADR 015's wave 4 no design system either — the tokens, the atoms and the `Emm*` widgets live in `:core:ui`. No Koin module here, and no exception: a new feature registers its module in `:presentation`'s `appModules()`.

Plain `com.android.library` (ADR 011), root package `com.emm.justchill.hh.<feature>`, `minSdk = 28`. Depends on `:presentation` (api), `:core:ui`, `:core:domain`, `:core:database`. A feature owns `hh/<feature>/`: its Screens plus `<Feature>Entries.kt` for nav wiring. Cross-feature: `hh/shared/` (nav host, routes, bottom bar). The tokens and the atoms come from `:core:ui`. Which atom and which token, and the routes rules: `.claude/rules/ui-components.md`, `.claude/rules/architecture.md`.

## Navigation shell

- The bottom bar holds four tabs plus the centre add button, no more. `HhBottomBar`'s 10sp labels already sit under the 4.5:1 AA floor; a fifth tab shrinks them further. A new destination swaps a tab out, never appends one.
- A shell screen never recomputes a number another screen owns. `ReportScreen` owns income, spend and balance; `SeeTransactionsScreen` owns the transaction list. Before adding a section, find the owner.
- `danger` on a pending recurring row's overdue period label (`PendingRecurringComponents.kt`, `ConfirmRecurringSheet.kt`) and on `SavingsRateBlock`'s deficit rate is deliberate: both are broken states, not amounts. The trend pill beside the rate is a comparison and stays `PillTone.Neutral`.
- `hh/shared/PlatformHostActions.kt`'s flags (`supportsBackup`, `showGoogleSignIn`, …) are plain constants the nav entries read; collapsing that seam is a separate change.

## Build conventions

- Every Compose artifact is governed by `androidx-compose-bom`; never add a `version.ref` to a Compose library. `koin-compose` pulls a few `org.jetbrains.compose.*` shims whose only dependency is the matching `androidx.compose.*` artifact; swapping to `koin-androidx-compose` would touch every injection site.
- Resources are ordinary Android resources under `src/main/res/`, reached through `com.emm.justchill.shared.R`; the namespace is pinned to `com.emm.justchill.shared` because the derived one would move that `R`. The bundled fonts left with the theme and are `:core:ui`'s own resources now.
- `SnackbarHostState` lives in the root `Scaffold` of `AppNavHost.kt`. `compose_stability.conf` declares `:presentation`'s state classes, `:core:domain` values and `:core:ui`'s types stable; if a list stutters, read the compiler metrics first.
- Errors reach the user through `DomainException.toUserMessage()`, never a raw message.

## Testing

`./gradlew :ui-android:testDebugUnitTest`. Pure UI logic gets a plain function next to the screen and a test here (`commitHashUi()` is the pattern). There is no Compose UI test harness. The Koin graph test and the mapper suites belong to `:presentation`, the formatter and atom suites to `:core:ui`.
