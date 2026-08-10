# :ui-android — CLAUDE.md

Android's Compose UI module: screens, navigation, theme and widgets. It renders what
`:presentation` exposes and owns nothing else — no ViewModels, no DI, no formatters.

**Android-only** (`justchill.kmp.ios=false` in this module's gradle.properties — ADR 005): the iOS
app is native SwiftUI over `:presentation`'s JustChillKit framework.

Root package: `com.emm.justchill.{hh.<feature>, core, components}` — the SAME packages as
`:presentation` (the extraction never renamed packages), so same-package symbols across the module
boundary need explicit imports. `minSdk = 28`. Depends on `:presentation` (api), `:domain`, `:data`.

This is where Android feature UI happens. `:androidApp` is a thin shell around it.

## Where things live

Everything is in `commonMain` (kept as commonMain so a second Compose target stays possible; today
it compiles for exactly one). A feature owns `hh/<feature>/` — its Screens plus `<Feature>Entries.kt`
for nav wiring. Cross-feature: `hh/shared/` (nav host, bottom bar, atoms), `core/theme/`,
`components/`. `androidMain/` holds UI-only actuals.

## DI

None here. `appModules(platformModule)` / `bootstrapAppGraph` live in `:presentation`
(`core/AppGraph.kt`); the Android platform module lives in `:androidApp`. A new feature registers
its Koin module in `:presentation`'s `appModules()`, never here — and its ViewModel goes into
`AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS` (now in `:presentation`'s androidHostTest).

## expect/actual — keep it to one

`hh/shared/PlatformHostActions.kt` (export / import / share / email / open-privacy-policy) is the
only one. With Android as the sole target the actual is a formality; the expect stays so the
declaration survives a future second target. Do not add a second without a real platform reason.

## Navigation

`AppNavHost` (commonMain) on the JetBrains nav3-UI port — Android-only now, but the explicit
SavedState config stays load-bearing:

**Landmine:** every route the host can push MUST be registered in `NavSavedStateConfiguration.kt`,
else `rememberNavBackStack` crashes on process-death restore — invisible to the compiler and the
build gate. `NavSavedStateConfigurationTest` reflects over sealed `AppRoute` to catch it.

**Landmine (nav3 entry caching):** `NavEntry.content` closures are cached until the back stack
changes. Host state an entry reads must arrive as `() -> T` accessors, never by value — see the
result channels in `AppNavHost.kt` (`pendingCategory`, `pendingImportJson`).

## MVI (consumed, not owned)

ViewModels extend `MviViewModel<S, I, E>` from `:presentation` (`core/mvi/`). Screens are
callback-driven composables taking `state` + `onIntent`; `SnackbarHostState` lives in the root
`Scaffold` of `AppNavHost.kt`. State classes carry semantic ids (`iconId`/`colorId`) — resolve
them at render time via `CategoryResolve.kt`, never in a mapper.

**Stability:** `:presentation`'s state classes are external to this module's compose compilation
and carry no `@Stable`/`@Immutable`. `compose_stability.conf` (wired via
`stabilityConfigurationFiles`) declares them — and `:domain` values — stable. If a list screen
ever stutters, check compose compiler metrics before blaming the pattern.

## UI conventions

- Compose Multiplatform `1.11.1` with JetBrains Material3 `1.11.0-alpha07`. Tokens in `core/theme/`
  are the source of truth.
- Compose resources under `commonMain/composeResources/`.
- User-facing strings are **Spanish** (from `:presentation`'s `UiStrings`); code, comments and
  identifiers are **English**.
- Errors reach the user through `DomainException.toUserMessage()` (`:presentation` `core/error/`),
  never a raw exception message.

## Testing

- `./gradlew :ui-android:testAndroidHostTest` — JVM host tests. `--rerun` is a **per-task** option:
  with several tasks in one invocation it forces only the task it follows.
- Lives here: `AppNavigatorTest`, `NavSavedStateConfigurationTest`, `HighlightQuotedTest`. The Koin
  graph test and the formatter/mapper suites belong to `:presentation`.

## Gate

On the standard `qualityGate` (detekt + host tests + dev lint); the iOS compile leg runs through
`:domain`/`:data`/`:presentation`, not here. Never gate on plain `./gradlew detekt`: it is
`NO-SOURCE` on every KMP module.
