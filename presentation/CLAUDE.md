# :presentation — CLAUDE.md

The compose-free presentation layer: MVI core, every feature's ViewModel/UiState/Intent/Effect, the
Koin DI modules, formatters, `UiStrings` and the preferences ports. `:ui-android` sits on it as a
Gradle dependency.

Root packages: `com.emm.justchill.{core, hh.<feature>}` — unchanged from the extraction on purpose
(zero import churn in consumers). Android namespace: `com.emm.presentation`. minSdk 28.

## The one rule

**NO Compose dependency may ever appear here.** That is the module's reason to exist: it is the
structural guarantee that ViewModels never touch UI types. Models carry semantic ids (`iconId`,
`colorId`), never `ImageVector`/`Color` — resolution happens at render time in each UI
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

Plain `com.android.library` (ADR 011/E11-04) — one `src/main`, one `src/test`, no KMP plugin, no
`commonMain`/`androidMain` split.

`core/` holds `AppGraph` (`appModules`/`bootstrapAppGraph`), `DispatchersProvider`, `CommitHash.kt`
— the one DI contract that is Android-only on both ends (`androidPlatformModule` and `AppNavHost`
both live in Android-only modules; its KDoc owns the whole rationale, don't restate it elsewhere) —
and one directory per cross-cutting concern, read the directory rather than a list written here. A
feature owns `hh/<feature>/` (ViewModel + UiState + Intent + Effect + `toUi` mappers) and one Koin
module in `hh/di/`; pure helpers and `UiStrings` sit in `hh/shared/`.

## Testing

- `./gradlew :presentation:testDebugUnitTest`.
- `src/test/` holds both what used to be two source sets: pure `kotlin.test` suites (formatters,
  mappers, copy) alongside anything needing MockK, JDBC SQLite or `Dispatchers.setMain`. Home of
  `core/AppGraphKoinTest.kt`, which resolves the WHOLE Koin graph off-device against
  `TestPlatformModule`. A missing binding compiles clean and passes the Android build — this test
  is the only net before a user hits it. Register every new ViewModel there.
  **What it cannot see is a definition nothing in the graph resolves.** The sweep iterates the Koin
  registry, so deleting a `single { }` whose only consumer is a direct `koinInject` / `koin.get`
  outside `appModules()` compiles, keeps the suite green, and crashes at that call site. Two named
  tests plug the two ways in — `EXPECTED_VIEW_MODELS` for ViewModels, `every single
  bootstrapAppGraph resolves is bound` for what the bootstrap resolves — and everything else is
  still exposed. Today that is exactly one binding: `CommitHash` (`core/CommitHash.kt`, sole
  consumer `koinInject<CommitHash>()` in `AppNavHost`), covered by `AndroidPlatformModuleTest` in
  `:androidApp` instead. Add a binding with a consumer outside the graph and you owe it a test of
  its own.
  It also guards what a definition *receives*, not just that it resolves: with a sentinel `Clock`
  and `TimeZone` bound, every `Clock`/`TimeZone` field on a `com.emm.` class must be the bound
  instance. That is the net for a hand-written `viewModel { }`/`factory { }` block forgetting a
  `get()` — `profileModule` did exactly that, and resolution-only tests never noticed.
- **A ViewModel that injects `TodayFlow` takes a fake in its tests** — collecting the real
  `ClockTodayFlow` inside `runTest` hangs instead of failing, because its self-rescheduling `delay`
  shares the test scheduler and `advanceUntilIdle()` never returns.
- MockK is test-only: nothing from `src/test` may leak into `src/main`.

## Gate

On the standard `qualityGate` via the convention plugin: detekt (`detektMain`, aggregating
`detektDebug`/`detektRelease` over `src/main`, plus `detektTest`) and `testDebugUnitTest`.
