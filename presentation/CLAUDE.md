# :presentation — CLAUDE.md

The compose-free presentation layer: MVI core, every feature's ViewModel/UiState/Intent/Effect, the
Koin DI modules, formatters, `UiStrings` and the preferences ports. Both UIs sit on it —
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

Because of that, `:ui-android` declares this module's state classes stable on its side rather than
here (`ui-android/CLAUDE.md`). Keep them immutable (`val` + immutable collections) or that
declaration becomes a lie.

## Where things live

`commonMain/core/` holds `AppGraph` (`appModules`/`bootstrapAppGraph`), `DispatchersProvider`, and
one directory per cross-cutting concern — read the directory rather than a list written here. A
feature owns `hh/<feature>/` (ViewModel + UiState + Intent + Effect + `toUi` mappers) and one Koin
module in `hh/di/`; pure helpers and `UiStrings` sit in `hh/shared/`.

`androidMain/` holds the lifecycle actuals under `core/lifecycle/`, plus `core/CommitHash.kt` — the
one DI contract that is Android-only on both ends, deliberately outside `commonMain` so it never
reaches the iOS compile or `JustChillKit`. Its KDoc owns the whole rationale (why a value class and
not a qualifier, why this source set); don't restate it elsewhere.
`iosMain/` holds its
counterpart, `KoinIos.kt` — the iOS entry point (`initKoin`, `iosPlatformModule`, and one typed
resolver per Swift-facing ViewModel) — and, unlike `androidMain`, two ordinary port implementations
that Android satisfies from `:androidApp` instead: `PrintlnDiagnosticsLogger` for `DiagnosticsLogger` and
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

- `./gradlew :presentation:testAndroidHostTest`.
- `androidHostTest/` — anything needing MockK, JDBC SQLite or `Dispatchers.setMain`; home of
  `core/AppGraphKoinTest.kt`, which resolves the WHOLE Koin graph off-device against
  `TestPlatformModule`. A missing binding compiles clean and passes the Android build — this test
  is the only net before a user hits it. Register every new ViewModel there.
  **What it cannot see is a definition nothing in the graph resolves.** The sweep iterates the Koin
  registry, so deleting a `single { }` whose only consumer is a direct `koinInject` / `koin.get`
  outside `appModules()` compiles, keeps the suite green, and crashes at that call site. Two named
  tests plug the two ways in — `EXPECTED_VIEW_MODELS` for ViewModels, `every single
  bootstrapAppGraph resolves is bound` for what the bootstrap resolves — and everything else is
  still exposed. Today that is exactly one binding: `CommitHash` (`androidMain/core/CommitHash.kt`,
  sole consumer `koinInject<CommitHash>()` in `AppNavHost`), covered by `AndroidPlatformModuleTest` in `:androidApp` instead.
  Add a binding with a consumer outside the graph and you owe it a test of its own.
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
