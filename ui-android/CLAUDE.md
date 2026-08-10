# :ui-android — CLAUDE.md

Android's Compose UI module: screens, navigation, theme and widgets. **Android-only since slice S2**
(`justchill.kmp.ios=false` in this module's gradle.properties — ADR 005): the iOS app is native
SwiftUI over `:presentation`'s JustChillKit framework. The ViewModels, MVI core, Koin DI, formatters
and `UiStrings` this module used to own live in `:presentation` since slice S1; this module renders
what `:presentation` exposes.

Root package: `com.emm.justchill.{hh.<feature>, core, components}` — the SAME packages as
`:presentation` (the extraction never renamed packages), so same-package symbols across the module
boundary need explicit imports. `minSdk = 28`. Depends on `:presentation` (api), `:domain`, `:data`.

This is where Android feature UI happens. `:androidApp` is a thin shell around it.

## Where things live

```
commonMain/
  hh/<feature>/     Screens + <Feature>Entries.kt (nav wiring) for: account auth category
                    home onboarding profile recurring report seetransactions transaction
  hh/shared/        AppNavHost, AppNavigator, HhRoutes, NavSavedStateConfiguration,
                    NavHostBindings, HhBottomBar, SyncEventsHandler,
                    PlatformHostActions (expect), UI atoms (EmmDropDown, LabelTextField…)
  hh/<feature>/CategoryResolve.kt   render-time resolution of :presentation's semantic
                    iconId/colorId into ImageVector/CategoryColor
  core/             theme/, ui/atoms/
  components/       cross-feature widgets
androidMain/        PlatformHostActions.android.kt (SAF launchers), ResumeEvents android actual
                    lives in :presentation — this module has UI-only actuals
```

("commonMain" survives the Android-only flip so a future second Compose target stays possible;
today it compiles for exactly one target.)

## DI

None here. `appModules(platformModule)` / `bootstrapAppGraph` live in `:presentation`
(`core/AppGraph.kt`); the Android platform module lives in `:androidApp`. A new feature registers
its Koin module in `:presentation`'s `appModules()`, never here — and its ViewModel goes into
`AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS` (now in `:presentation`'s androidHostTest).

## expect/actual — keep it to one

| Declaration | Why |
|---|---|
| `hh/shared/PlatformHostActions.kt` | export / import / share / email / open-privacy-policy |

(`ResumeEvents` moved to `:presentation` with the sync port.) With Android as the only target the
actual is a formality; the expect stays so the declaration survives a future second target.

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
- Lives here: `AppNavigatorTest`, `NavSavedStateConfigurationTest`, `HighlightQuotedTest`.
  The Koin graph test and the formatter/mapper suites moved to `:presentation`.

## Gate

On the standard `qualityGate` (detekt + host tests + dev lint). The iOS compile leg left with the
iOS targets — it runs through `:domain`/`:data`/`:presentation` now. Never gate on plain
`./gradlew detekt`: it is `NO-SOURCE` on every KMP module.
