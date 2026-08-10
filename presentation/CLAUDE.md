# :presentation — CLAUDE.md

The compose-free presentation layer, extracted from `:shared-ui` in slice S1 of
`docs/swiftui/PLAN.md` (ADR 005): MVI core, every feature's ViewModel/UiState/Intent/Effect,
the Koin DI modules, formatters, `UiStrings` and the sync/preferences ports. Both UIs sit on it —
`:shared-ui` (Android Compose) as a Gradle dependency, the SwiftUI iOS app through the
**JustChillKit** framework this module declares (static, SKIE-processed, exports `:domain` +
`:data`).

Root packages: `com.emm.justchill.{core, hh.<feature>}` — unchanged from the extraction on purpose
(zero import churn in consumers). Android namespace: `com.emm.presentation`. minSdk 28.

## The one rule

**NO Compose dependency may ever appear here.** That is the module's reason to exist: it restored
the structural guarantee (lost in slice H) that ViewModels never touch UI types, and it is what
makes the module exportable to Swift. Models carry semantic ids (`iconId`, `colorId`), never
`ImageVector`/`Color` — resolution happens at render time in each UI
(`shared-ui .../CategoryResolve.kt` on Android). If a state class needs something visual, it
carries the id and the UI resolves it.

Stability note: `:shared-ui` compensates the missing `@Stable`/`@Immutable` annotations via
`shared-ui/compose_stability.conf` (`stabilityConfigurationFiles`) — state classes here are
declared stable there. Keep state classes immutable (`val` + immutable collections) or that
declaration becomes a lie.

## Where things live

```
commonMain/
  core/             AppGraph (appModules/bootstrapAppGraph), mvi/, error/, format/,
                    preferences/, sync/ (incl. expect resumeEvents), Result/FlowResult,
                    SupabaseConfig, DispatchersProvider
  hh/<feature>/     XxxViewModel + XxxUiState + XxxIntent + XxxEffect + toUi mappers
  hh/di/            one Koin module per feature + data/supabase/sync/auth wiring
  hh/shared/        UiStrings, CurrencyFormat, NumberFormatEs, SpanishDateFormat,
                    SpanishSearch, MonthLabels, DefaultUniqueIdProvider, pure helpers
androidMain/        ResumeEvents.android.kt (ProcessLifecycleOwner)
iosMain/            KoinIos.kt (initKoin + iosPlatformModule + Swift-facing VM resolvers),
                    ResumeEvents.ios.kt, PrintlnSyncLogger, UnavailableGoogleSignInLauncher,
                    generated IosSupabaseConfig (justchill.ios.supabase.config)
```

## Framework / SKIE

- `JustChillKit` is declared in this module's build file; iosApp's Xcode script phase runs
  `:presentation:embedAndSignAppleFrameworkForXcode`.
- SKIE (version in the catalog, analytics disabled): sealed → Swift enums, `Flow`/`StateFlow` →
  `AsyncSequence`, suspend → async. It runs on the **link** tasks, not `compileKotlinIos*` — a
  green compile has not exercised SKIE; a green `linkDebugFrameworkIosSimulatorArm64` has.
- Kotlin upgrades now wait for SKIE's support window — check the SKIE↔Kotlin matrix before
  bumping `kotlinVersion`.
- Swift sees top-level functions per file facade (`KoinIosKt.*`); `init*` names are mangled
  (`initKoin` → `doInitKoin`). Kotlin `description` properties collide with `NSObject.description`
  and surface as `description_`.
- Swift cannot call Koin's reified `get()` — each ViewModel the Swift side needs gets a typed
  resolver in `KoinIos.kt` (`fun seeTransactionsViewModel(): SeeTransactionsViewModel`).

## Testing

- `./gradlew :presentation:testAndroidHostTest` (JVM host tests; `--rerun` is per-task).
- `androidHostTest/` — anything needing MockK, JDBC SQLite or `Dispatchers.setMain`; home of
  `core/AppGraphKoinTest.kt`, which resolves the WHOLE Koin graph off-device against
  `TestPlatformModule`. A missing binding compiles clean and passes the Android build — this test
  is the only net before a user hits it. Register every new ViewModel there.
- `commonTest/` — pure `kotlin.test` suites (formatters, mappers, copy).
- MockK is JVM-only: nothing from `androidHostTest` may leak into `commonMain` (the iOS compile
  gate breaks).

## Gate

On the standard `qualityGate` via the convention plugin: detekt (main/iosMain source sets), host
tests, and — on macOS — the iOS compiles. This module now carries the compile-gate invariant that
`:shared-ui` used to (ADR 003 → ADR 005): the exported core stays free of `java.*`/`android.*`.
