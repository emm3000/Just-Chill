---
paths:
  - "**/src/*/kotlin/**"
---

# Architecture rules

Clean Architecture across the module layout in `CLAUDE.md`. Gradle enforces the module direction; these rules explain it and cover what Gradle cannot see.

## Layers and dependency direction

| Layer | Contains |
|---|---|
| `:core:domain` | Pure Kotlin. Models, value objects, use cases, and the **interfaces** the outer layers implement. |
| `:core:database` | Implementations of the domain interfaces: SQLDelight, mappers, the `SnapshotStore` over the six tables. |
| `:core:backup` | The snapshot file and its account: DTOs, decoder, Supabase Storage, the backup cycle, auth. |
| `:presentation` | Compose-free MVI core, ViewModels with their `UiState` / `Intent` / `Effect`, Koin modules, formatters, `UiStrings`. |
| `:ui-android` | Compose screens, navigation, theme tokens and atoms. |
| `:androidApp` | `MainActivity`, `EmmApp`, the platform Koin module, flavors, shortcuts, the session keystore. |

Allowed dependencies, and nothing else:

```
androidApp   -> ui-android, presentation, core:backup, core:database, core:domain
ui-android   -> presentation, core:database, core:domain
presentation -> core:backup, core:database, core:domain
core:backup  -> core:domain
core:database -> core:domain
```

- `:core:domain` is pure Kotlin (`kotlin("jvm")`): `kotlinx-coroutines-core` and `kotlinx-datetime` only. No Android, no SQLDelight, no Supabase, no Ktor. `android.*` cannot resolve there; the rest is convention, reviewed.
- Whatever asks "what day is it" takes an injected `Clock` **and** an injected `TimeZone`, and neither parameter carries a default: a default never blocks an explicit argument, so a test passing a fake clock also passes against the ambient one. `hh/di/SharedModule.kt` is the only place a clock or a zone enters the graph; `AppGraphKoinTest` asserts by identity that every graph-built `com.emm.` class holds the bound instances. `TodayFlow.today()` is the one way a ViewModel derives the date.
- `:presentation` depends on `:core:database` and `:core:backup` for one reason: the Koin modules in `hh/di/` bind interface to implementation in one place. `SnapshotStore` is bound there too, which is what keeps `:core:backup` off `:core:database`. A ViewModel takes `:core:domain` interfaces, never a SQLDelight type or a `Default*` implementation.
- `:ui-android` and `:androidApp` production code import no `:core:domain` repository; the leak stops at `:presentation`.
- SQLDelight on device is the source of truth for reads and writes. Supabase holds snapshot backups (ADR 009); nothing reads rows from it. A snapshot crosses the two modules as a `LocalSnapshot` of domain models, never as a SQLDelight row or a DTO.

## Dependency inversion is the seam

The domain declares the contract; the infrastructure obeys it. The domain never imports an implementation.

- Repository interfaces (`{Entity}Repository`) live in `:core:domain`. Implementations (`Default{Entity}Repository` over a `{Entity}LocalDataSource`) live in `:core:database`.
- A platform capability `:presentation` needs (`GoogleSignInLauncher`, `DispatchersProvider`) is an interface in `:presentation`, implemented in `:androidApp` and bound in `androidPlatformModule`.

## A use case only where there is domain logic

A pure read goes from the ViewModel straight to the `Repository` interface. A use case whose whole body is one `repository.x(...)` call on a read is a rename, not a layer. Writes earn one far more often, because a write is where the invariants are.

Loan writes always go through `CreateLoanUseCase` / `UpdateLoanUseCase`: `LoanRepository.create` / `update` accept a fully built `Loan`, so nothing compiles against the rule. A ViewModel calling them fails review, however well-formed the `Loan` looks.

## Errors

Sealed `DomainException` (`core/domain/.../shared/error/`) is the one failure type. `:core:database`'s `shared/SafeCall.kt` (`safeDbCall`, `catchAsDomainException`) translates SQLDelight exceptions into it; `:presentation`'s `core/error/DomainExceptionExt.kt` renders the Spanish message. Add a failure mode by extending `DomainException`, never with a new exception type.

Every catch-all owes a `CancellationException` arm first. `runCatching` and `catch (e: Exception)` both swallow it, the body runs on, and a cancelled loader overwrites the winner. Rethrow cancellation, then catch `Exception` (`MviViewModel.launchSafe` is the pattern); a `Flow.catch` lambda owes the arm explicitly.

## Each layer owns its own model

A SQLDelight row, a domain model and a `UiState` are three different things even when their fields match. Mappers convert between them (`{entity}Mappers.kt` in `:core:database`, `toUi` mappers in `:presentation`).

- A SQLDelight row never reaches a `UiState`.
- A domain model never carries presentation concerns (formatted strings, resource ids, colors).
- A presentation model carries a semantic id (`iconId`, `colorId`), never an `ImageVector` or a `Color`; `:ui-android` resolves it at render time (`hh/transaction/CategoryResolve.kt`).

This is not duplication to be removed. See `principles.md`, DRY.

## MVI contract

Naming lives in `naming.md`. This is the flow. The base class is `mvi/MviViewModel.kt` in `:core:ui`.

- **One state object per feature.** `<Feature>UiState : UiState` is a `data class` (or a `sealed interface` of data classes) with every field `val` and immutable collections. `:ui-android` declares these classes stable in `compose_stability.conf`; a `var` or a `MutableMap` turns that declaration into a lie no compiler catches.
- **One public entry point.** `MviViewModel<S, I, E>` exposes `state: StateFlow<S>`, `effect: Flow<E>` and `onIntent(intent: I)`. A screen reaches its ViewModel through those three and nothing else.
- **State is a `StateFlow`, effects are one-shot.** Effects (navigation, snackbars) go through the buffered channel and are consumed once. An effect is never stored in `UiState`, because state replays on recomposition and would fire it twice.
- **State stores what the user chose, never what was resolved.** A selection is an id; the resolved object is a getter over the catalog held in the same state. A stored resolved object is a cache with no invalidation, and it is how a movement gets filed under a deleted category. A save writes the resolved selection, never the raw id.
- **Effects ride a `Channel`, never a `SharedFlow`.** Collectors are `LaunchedEffect(vm)` inside an entry and die when it is buried; the channel buffers until the next one, a replay-0 `SharedFlow` drops. An effect is a navigation or a transient message; anything the UI keeps rendering (a sheet, a dialog, a focus) is state.
- **A visibility flag lives in `UiState`, never in `remember` / `rememberSaveable`** (ADR 012). One sheet field per screen: an enum when sheets are mutually exclusive, a `Boolean` when there is one.
- **A ViewModel never switches dispatcher.** No `withContext` or `Dispatchers.` in `hh/`; `:core:database` owns its threading.
- **`launchSafeIn` retries a collector three times (200/400/800 ms), then dies for the ViewModel's life.** It heals lock contention, not a persistent failure; tab ViewModels outlive `switchTab`, so a dead collector stays dead. `stateIn` / `shareIn` upstreams bypass the funnel: today all are fed by `todayFlow()` alone, and a data-backed one has no error door.
- **A `when` helper split out of `onIntent` takes a nested sealed sub-interface, never the wide intent type with an `else`** (`onPaymentFormIntent`, `onScreenChromeIntent`): the `else` swallows a new intent forgotten in the helper.
- **The screen is callback-driven.** `<Feature>Screen` collects `state`, hands `vm::onIntent` down to a private stateless content composable, and consumes effects in a `LaunchedEffect(vm)`. It never touches a repository or a use case.
- **The entry wires navigation.** `<Feature>Entries.kt` registers `entry<Route>` in `AppNavHost`'s `entryProvider`, obtains an `AppNavigator` with `rememberAppNavigator`, and passes host state as lambdas.
- **ViewModels never hold literal UI copy.** A ViewModel emits an enum or another typed value (`AuthEffect.Notify(AuthMessage.ConfirmationLinkResent)`), never a Spanish string; the screen resolves it to text. Shared copy lives in `:presentation`'s `hh/shared/UiStrings.kt`.
- **Intents describe what the user did**, not what the ViewModel should do. Local UI state with no business meaning (an expanded section) may stay as `remember` inside the composable.
- `:presentation` and `:ui-android` share package names on purpose. A same-package symbol that crosses the module boundary is imported explicitly.

## `:presentation` is compose-free by review, not by compiler

`androidx.lifecycle` is the only androidx artifact the module holds. A Compose import compiles and the gate stays green, so the check is a grep that returns nothing:

```
rg -e 'androidx\.compose' -e 'BuildConfig' -e '\bR\.' -e 'stringResource|painterResource|Font\(R\.' -e '@Preview|tooling\.preview' -e 'LocalConfiguration' -e 'koin\.androidx' presentation/src/main
```

MockK never leaks into `src/main` either.

## Koin

- A binding is registered exactly once: a feature module in `hh/di/`, listed in `appModules()` (`core/AppGraph.kt`); a platform binding in `:androidApp`'s `androidPlatformModule`. `startKoin` is called only in `:androidApp`.
- Every new ViewModel goes into `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`. A binding whose only consumer is a `koinInject` / `koin.get` outside the graph owes its own test.

## Routes and the back stack

Routes live in `ui-android/.../hh/shared/HhRoutes.kt` as subtypes of sealed `AppRoute`.

- **Every route the host can push is `@Serializable`, fields included.** `rememberNavBackStack` stores each entry by class name and re-resolves it through `Class.forName(name).kotlin.serializer()`, so an unserializable route crashes on process-death restore and nowhere else. `RouteSerializationTest` reflects over `AppRoute` and round-trips every subtype; a route declared outside the hierarchy opts out of that guard.
- **One door is no door.** A destination reachable through exactly one entry point is unreachable the moment that entry is gated. Gate the content of an entry point, never its existence. Before deleting a row that pushes a route, `rg` the route and confirm a second door exists.
- **Moving to a route that may already be on the stack uses `AppNavigator.pushToTop`, never `push`.** `push` guards with `backStack.contains(route)`, a duplicate guard that silently does nothing once the target is buried. `pushToTop` pops what sits above and reveals or replaces the route.
- **`NavEntry.content` closures are cached until the back stack changes.** Host state an entry reads arrives as a `() -> T` accessor, never by value; the result channels in `AppNavHost.kt` (`pendingCategory`, `pendingImportJson`) are the pattern.
- A route promoted to a tab changes supertype to `BottomBarRoute` and loses its back affordance.

## When a new dependency crosses a layer

Before adding a dependency to any package, check the direction above. If the change needs `:core:domain` to reach outward, the design is wrong: invert it with an interface in `:core:domain`.
