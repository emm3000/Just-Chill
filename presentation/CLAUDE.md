# :presentation — CLAUDE.md

The compose-free presentation layer: MVI core, every feature's ViewModel/UiState/Intent/Effect, the
Koin DI modules, formatters, `UiStrings` and the sync/preferences ports. Both UIs sit on it —
`:ui-android` (Android Compose) as a Gradle dependency, the SwiftUI iOS app through the
**JustChillKit** framework this module declares (static, SKIE-processed, exports `:domain` +
`:data`).

Root packages: `com.emm.justchill.{core, hh.<feature>}` — unchanged from the extraction on purpose
(zero import churn in consumers). Android namespace: `com.emm.presentation`. minSdk 28.

## The one rule

**NO Compose dependency may ever appear here.** That is the module's reason to exist: it is the
structural guarantee that ViewModels never touch UI types, and it is what makes the module
exportable to Swift. Models carry semantic ids (`iconId`, `colorId`), never
`ImageVector`/`Color` — resolution happens at render time in each UI
(`ui-android .../CategoryResolve.kt` on Android). If a state class needs something visual, it
carries the id and the UI resolves it.

**ViewModel purity** is the convention that sits beside it: a ViewModel takes `:domain` interfaces,
never SQLDelight types or a `Default*` implementation. Unlike the no-Compose rule this one is only
reviewed, not structural — the module *does* depend on `:data`, deliberately, so the Koin wiring can
exist once instead of once per platform.

Stability note: `:ui-android` compensates the missing `@Stable`/`@Immutable` annotations via
`ui-android/compose_stability.conf` (`stabilityConfigurationFiles`) — state classes here are
declared stable there. Keep state classes immutable (`val` + immutable collections) or that
declaration becomes a lie.

## Where things live

`commonMain/core/` holds `AppGraph` (`appModules`/`bootstrapAppGraph`), `mvi/`, `error/`, `format/`,
`preferences/`, `sync/` and `DispatchersProvider`. A feature owns `hh/<feature>/` (ViewModel +
UiState + Intent + Effect + `toUi` mappers) and one Koin module in `hh/di/`; pure helpers and
`UiStrings` sit in `hh/shared/`.

`androidMain/` holds the two lifecycle actuals, `core/lifecycle/ResumeEvents.android.kt` and
`core/lifecycle/BackgroundEvents.android.kt`, plus `core/CommitHash.kt` — an ordinary Android-only DI
contract (producer and consumer are both Android), deliberately outside `commonMain` so it never
reaches the iOS compile or `JustChillKit`.
`iosMain/` holds its
counterpart, `KoinIos.kt` — the iOS entry point (`initKoin`, `iosPlatformModule`, and one typed
resolver per Swift-facing ViewModel) — and, unlike `androidMain`, two ordinary port implementations
that Android satisfies from `:androidApp` instead: `PrintlnSyncLogger` for `SyncLogger` and
`UnavailableGoogleSignInLauncher` for the still-deferred iOS Google sign-in. Both are bound in
`iosPlatformModule`.

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
  It also guards what a definition *receives*, not just that it resolves: with a sentinel `Clock`
  and `TimeZone` bound, every `Clock`/`TimeZone` field on a `com.emm.` class must be the bound
  instance. That is the net for a hand-written `viewModel { }`/`factory { }` block forgetting a
  `get()` — `profileModule` did exactly that, and resolution-only tests never noticed.
- `commonTest/` — pure `kotlin.test` suites (formatters, mappers, copy).
- MockK is JVM-only: nothing from `androidHostTest` may leak into `commonMain` (the iOS compile
  gate breaks).

## Gate

On the standard `qualityGate` via the convention plugin: detekt (main/iosMain source sets), host
tests, and — on macOS — the iOS compiles. This module carries the compile-gate invariant (ADR 005):
the exported core stays free of `java.*`/`android.*`.
