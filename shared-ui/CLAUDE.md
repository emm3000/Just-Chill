# :shared-ui — CLAUDE.md

Compose Multiplatform module: **all** the app's UI, ViewModels, and Koin wiring, shared by Android
and iOS. Targets `android` (host tests only) + `iosArm64` + `iosSimulatorArm64`.

Root package: `com.emm.justchill.{hh.<feature>, core, components}`. `minSdk = 28`.
Depends on `:domain` and `:data`.

This is where feature work happens. `:androidApp` and `iosApp/` are thin shells around it.

## Where things live

```
commonMain/
  hh/<feature>/     account auth category home onboarding profile
                    recurring report seetransactions transaction
  hh/shared/        AppNavHost, HhRoutes, HhBottomBar, NavSavedStateConfiguration,
                    PlatformHostActions (expect), formatters, UiStrings
  hh/di/            one Koin module per feature + supabase/sync/auth/data wiring
  core/             AppGraph, mvi/, theme/, error/, format/, preferences/, sync/, ui/atoms/
  components/       cross-feature widgets
androidMain/        Android actuals only
iosMain/            MainViewController, KoinIos, iOS actuals
```

## DI (`core/AppGraph.kt`)

`appModules(platformModule)` returns the ONE shared module list; `bootstrapAppGraph(koin)` runs the
shared post-`startKoin` sequence (claim-on-sign-in observer + `SyncOrchestrator.start()`). Both
platforms call them.

`startKoin {}` itself is NOT shared — Android needs `androidContext()` / `androidLogger()` from
koin-android, absent in commonMain. Only the module list and the bootstrap are common.

A new feature registers its module in `appModules()`, **never** in `EmmApp` or `KoinIos`.

Platform-specific singles live in the injected `platformModule` (`AndroidPlatformModule.kt` /
`KoinIos.kt`): DB driver + seed, `Settings` backend, `SupabaseConfig`, `appVersion`,
`googleServerClientId`, `GoogleSignInLauncher`, `DispatchersProvider`, `CurrentActivityHolder`.

**Koin failures are runtime-only** — a missing or wrongly qualified binding compiles clean and
passes `assembleDevDebug`, then crashes when the user opens the screen.
`androidHostTest/core/AppGraphKoinTest.kt` resolves the entire graph off-device to catch exactly
that. Add every new ViewModel to its `EXPECTED_VIEW_MODELS` list.

## expect/actual — keep it to two

| Declaration | Why |
|---|---|
| `hh/shared/PlatformHostActions.kt` | export / import / share / email / open-privacy-policy |
| `core/sync/ResumeEvents.kt` | Android `ProcessLifecycleOwner` vs iOS `NSNotificationCenter` |

Everything else is common. Before adding a third, hoist the platform bit to a callback the nav host
supplies instead.

## Navigation

ONE commonMain `AppNavHost` on the JetBrains nav3-UI port, for both platforms (slice F `186d3b6`
reversed the earlier Android/iOS split — ignore the "Option A" text in `docs/kmp/PHASE_3_SPEC.md`).

**Landmine:** every route the host can push MUST be registered in `NavSavedStateConfiguration.kt`.
Kotlin/Native has no reflective serializer discovery, and Android now uses the same explicit config,
so a missing entry crashes `rememberNavBackStack` on process-death restore — invisible to the
compiler and to the Android build gate.

## MVI

ViewModels extend `MviViewModel<S, I, E>` (`core/mvi/`). Per feature: `XxxViewModel`, `XxxUiState`,
`XxxIntent`, `XxxEffect`. Screens are callback-driven composables taking `state` + `onIntent`.
`SnackbarHostState` lives in the root `Scaffold` of `AppNavHost.kt`.

ViewModel purity — VMs take `:domain` interfaces, never SQLDelight or `Default*` types — is
**convention only** since commonMain gained the `:data` dependency (slice H). The module boundary no
longer enforces it. Review for it.

## UI conventions

- Compose Multiplatform `1.11.1` with JetBrains Material3 `1.11.0-alpha07`. Tokens in `core/theme/`
  are the source of truth.
- Compose resources under `commonMain/composeResources/`.
- User-facing strings are **Spanish**; code, comments and identifiers are **English**.
- Errors reach the user through `core/error/DomainExceptionExt.kt` (`toUserMessage()`), never a raw
  exception message.

## Testing

- `./gradlew :shared-ui:testAndroidHostTest` — JVM host tests (JUnit4, MockK, coroutines-test).
  Add `--rerun`; the task goes `UP-TO-DATE` across sessions.
- `commonTest/` for platform-neutral assertions (`kotlin.test`); `androidHostTest/` for anything
  needing MockK, a JDBC SQLite driver, or `Dispatchers.setMain`.
- Nothing in `androidHostTest/` may leak into commonMain — it is JVM-only and would break the iOS compile.

## Gate

Before shipping a slice, run the reinforced gate in `docs/kmp/ORCHESTRATION.md`. The two that matter
most here: `:shared-ui:compileKotlinIosSimulatorArm64` (proves zero `java.*`/`android.*` leak) and
`:shared-ui:testAndroidHostTest` (proves the Koin graph resolves). The pre-push hook does NOT cover
this module — plain `./gradlew detekt` is `NO-SOURCE` on every KMP module.
