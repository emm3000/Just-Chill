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
| `:core:ui` | The MVI base, the navigation vocabulary (`AppRoute`, `CaptureRoute`, `BottomBarRoute`, `AppNavigator`, `rememberAppNavigator`, `NavHostBindings`, the `PlatformHostActions` interface) in `navigation/`, the error copy (`DomainException.toUserMessage()`) in `error/`, the Spanish money, date and search formatters, the design system (theme tokens, atoms, `Emm*` widgets, fonts), and the `PersonBalanceUi` model with its owed-total helpers in `loan/`. |
| `:core:testing` | JVM test fixtures on `:core:domain` alone; wired into feature modules, `:core:ui` and `:androidApp` as `testImplementation`. Fixture list: `core/testing/CLAUDE.md`. |
| `:feature:*` | One screen family: its Compose-free ViewModels, its Compose screens and nav entries, its `@Serializable` routes and its Koin module. |
| `:androidApp` | `MainActivity`, `EmmApp`, the app shell (nav host, entry graph, shortcut routes, the SAF host actions), the Koin graph with the cross-cutting modules in `core/di/` and one wiring file per feature, the backup orchestrator and the lifecycle and preference ports in `core/`, the platform Koin module, flavors, shortcuts, the session keystore. |

Allowed dependencies, and nothing else:

```
androidApp   -> feature:*, core:backup, core:database, core:ui, core:domain
feature:*    -> core:ui, core:domain, core:testing
core:backup  -> core:domain
core:database -> core:domain
core:ui      -> core:domain (+ core:testing, testImplementation)
core:testing -> core:domain
```

`checkModuleBoundaries` fails the gate on any other edge; only `:androidApp` may depend on a feature.

- `:core:domain` is pure Kotlin (`kotlin("jvm")`): `kotlinx-coroutines-core` and `kotlinx-datetime` only. No Android, no SQLDelight, no Supabase, no Ktor. `android.*` cannot resolve there; the rest is convention, reviewed.
- Whatever asks "what day is it" takes an injected `Clock` **and** an injected `TimeZone`, and neither parameter carries a default: a default never blocks an explicit argument, so a test passing a fake clock also passes against the ambient one. `:androidApp`'s `core/di/SharedModule.kt` is the only place a clock or a zone enters the graph; `AppGraphKoinTest` asserts by identity that every graph-built `com.emm.` class holds the bound instances. `TodayFlow.today()` is the one way a ViewModel derives the date.
- `:androidApp` depends on `:core:database` and `:core:backup` for one reason: the modules in `core/di/` bind interface to implementation in one place. `SnapshotStore` is bound there too, which is what keeps `:core:backup` off `:core:database`. A ViewModel takes `:core:domain` interfaces, never a SQLDelight type or a `Default*` implementation.
- Binding them is `:androidApp`'s `core/di/` and `wiring/`; nothing else names one. The leak check is `rg -l 'Default[A-Z][A-Za-z]*(Repository|DataSource)' feature/*/src/main androidApp/src/main --glob '!**/core/di/**' --glob '!**/wiring/**'`, and it returns nothing.
- SQLDelight on device is the source of truth for reads and writes. Supabase holds snapshot backups (ADR 009); nothing reads rows from it. A snapshot crosses the two modules as a `LocalSnapshot` of domain models, never as a SQLDelight row or a DTO.

## Dependency inversion is the seam

The domain declares the contract; the infrastructure obeys it. The domain never imports an implementation.

- Repository interfaces (`{Entity}Repository`) live in `:core:domain`. Implementations (`Default{Entity}Repository` over a `{Entity}LocalDataSource`) live in `:core:database`.
- A platform capability a feature needs (`GoogleSignInLauncher`) is an interface in that feature or in `:core:domain`, implemented in `:androidApp` and bound in `androidPlatformModule`.

## A use case only where there is domain logic

A pure read goes from the ViewModel straight to the `Repository` interface. A use case whose whole body is one `repository.x(...)` call on a read is a rename, not a layer. Writes earn one far more often, because a write is where the invariants are.

Loan writes always go through `CreateLoanUseCase` / `UpdateLoanUseCase`: `LoanRepository.create` / `update` accept a fully built `Loan`, so nothing compiles against the rule. A ViewModel calling them fails review, however well-formed the `Loan` looks.

## Errors

Sealed `DomainException` (`core/domain/.../shared/error/`) is the one failure type. `:core:database`'s `shared/SafeCall.kt` (`safeDbCall`, `catchAsDomainException`) translates SQLDelight exceptions into it; `:core:ui`'s `core/ui/error/DomainExceptionExt.kt` renders the Spanish message. Add a failure mode by extending `DomainException`, never with a new exception type.

Every catch-all owes a `CancellationException` arm first. `runCatching` and `catch (e: Exception)` both swallow it, the body runs on, and a cancelled loader overwrites the winner. Rethrow cancellation, then catch `Exception` (`MviViewModel.launchSafe` is the pattern); a `Flow.catch` lambda owes the arm explicitly.

## Each layer owns its own model

A SQLDelight row, a domain model and a `UiState` are three different things even when their fields match. Mappers convert between them (`{entity}Mappers.kt` in `:core:database`, `toUi` mappers beside the ViewModel that needs them).

- A SQLDelight row never reaches a `UiState`.
- A domain model never carries presentation concerns (formatted strings, resource ids, colors).
- A presentation model carries a semantic id (`iconId`, `colorId`), never an `ImageVector` or a `Color`; the render layer resolves it against the catalog (`:core:ui`'s `core/ui/category/CategoryResolve.kt`).

This is not duplication to be removed. See `principles.md`, DRY.

## MVI contract

Naming lives in `naming.md`. This is the flow. The base class is `mvi/MviViewModel.kt` in `:core:ui`.

- **One state object per feature.** `<Feature>UiState : UiState` is a `data class` (or a `sealed interface` of data classes) with every field `val` and immutable collections. A module whose screens read state declared elsewhere declares it stable in its `compose_stability.conf`; a `var` or a `MutableMap` turns that declaration into a lie no compiler catches.
- **One public entry point.** `MviViewModel<S, I, E>` exposes `state: StateFlow<S>`, `effect: Flow<E>` and `onIntent(intent: I)`. A screen reaches its ViewModel through those three and nothing else.
- **State is a `StateFlow`, effects are one-shot.** Effects (navigation, snackbars) go through the buffered channel and are consumed once. An effect is never stored in `UiState`, because state replays on recomposition and would fire it twice.
- **State stores what the user chose, never what was resolved.** A selection is an id; the resolved object is a getter over the catalog held in the same state. A stored resolved object is a cache with no invalidation, and it is how a movement gets filed under a deleted category. A save writes the resolved selection, never the raw id.
- **Effects ride a `Channel`, never a `SharedFlow`.** Collectors are `LaunchedEffect(vm)` inside an entry and die when it is buried; the channel buffers until the next one, a replay-0 `SharedFlow` drops. An effect is a navigation or a transient message; anything the UI keeps rendering (a sheet, a dialog, a focus) is state.
- **A visibility flag lives in `UiState`, never in `remember` / `rememberSaveable`** (ADR 012). One sheet field per screen: an enum when sheets are mutually exclusive, a `Boolean` when there is one.
- **A ViewModel never switches dispatcher.** No `withContext` or `Dispatchers.` in a ViewModel; `:core:database` owns its threading.
- **`launchSafeIn` retries a collector three times (200/400/800 ms), then dies for the ViewModel's life.** It heals lock contention, not a persistent failure; a ViewModel outlives the frames that feed it, so a dead collector stays dead. `stateIn` / `shareIn` upstreams bypass the funnel: today all are fed by `todayFlow()` alone, and a data-backed one has no error door.
- **A `when` helper split out of `onIntent` takes a nested sealed sub-interface, never the wide intent type with an `else`** (`onPaymentFormIntent`, `onScreenChromeIntent`): the `else` swallows a new intent forgotten in the helper.
- **The screen is callback-driven.** `<Feature>Screen` collects `state`, hands `vm::onIntent` down to a private stateless content composable, and consumes effects in a `LaunchedEffect(vm)`. It never touches a repository or a use case.
- **The entry wires navigation.** `<Feature>Entries.kt` registers `entry<Route>` in `AppNavHost`'s `entryProvider`, obtains an `AppNavigator` with `rememberAppNavigator(bindings.backStack)`, and passes host state as lambdas.
- **ViewModels never hold literal UI copy.** A ViewModel emits an enum or another typed value (`AuthEffect.Notify(AuthMessage.ConfirmationLinkResent)`), never a Spanish string; the screen resolves it to text.
- **Intents describe what the user did**, not what the ViewModel should do. Local UI state with no business meaning (an expanded section) may stay as `remember` inside the composable.

## A ViewModel is compose-free

`checkComposeFreeViewModels` is on the gate: it fails any `*ViewModel.kt` or `*UiState.kt` in the module carrying an `import androidx.compose` line. Every module runs it, `:androidApp` included.

What the task cannot see, and review does, over the same files:

```
rg -e 'BuildConfig' -e '\bR\.' -e 'stringResource|painterResource' -e '@Preview|tooling\.preview' -e 'LocalConfiguration' -e 'koin\.androidx' -g '*ViewModel.kt' -g '*UiState.kt' feature/*/src/main
```

MockK never leaks into `src/main` either.

## Koin

- A binding is registered exactly once. A feature exposes `<feature>Module` with its ViewModels only; `:androidApp`'s `wiring/<Feature>Wiring.kt` binds that feature's use cases and `includes` it. What no single feature owns is a module in `:androidApp`'s `core/di/`. All of them are listed in `appModules()` (`core/AppGraph.kt`), which is also where a platform binding's `androidPlatformModule` joins. `startKoin` is called only in `:androidApp`.
- Every new ViewModel goes into `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`. A binding whose only consumer is a `koinInject` / `koin.get` outside the graph owes its own test.

## Routes and the back stack

`AppRoute` is a plain interface in `:core:ui`'s `core/ui/navigation/AppRoute.kt`, alongside `CaptureRoute : AppRoute`, the marker `AddTransactionRoute` and `EditTransactionRoute` implement, and `BottomBarRoute : AppRoute`, the marker the four tab roots implement and nothing else (ADR 022). A feature declares its own concrete routes and exports them as `val <feature>Routes: List<KClass<out AppRoute>>`; `RouteSerializationTest` concatenates the eight registries.

- **Every route the host can push is `@Serializable`, fields included.** `rememberNavBackStack` stores each entry by class name and re-resolves it through `Class.forName(name).kotlin.serializer()`, so an unserializable route crashes on process-death restore and nowhere else. Routes are no longer a sealed hierarchy a reflection scan can enumerate: `RouteSerializationTest` (`:androidApp`) concatenates the route registries and asserts its hand-written samples cover exactly that union, then round-trips each sample. A route missing from its registry is never round-tripped.
- **One door is no door.** A destination reachable through exactly one entry point is unreachable the moment that entry is gated. Gate the content of an entry point, never its existence. Before deleting a row that pushes a route, `rg` the route and confirm a second door exists.
- **Moving to a route that may already be on the stack uses `AppNavigator.pushToTop`, never `push`.** `push` guards with `backStack.contains(route)`, a duplicate guard that silently does nothing once the target is buried. `pushToTop` pops what sits above and reveals or replaces the route.
- **`NavEntry.content` closures are cached until the back stack changes.** Host state an entry reads arrives as a `() -> T` accessor, never by value; the result channels in `AppNavHost.kt` (`pendingCategory`, `pendingImportJson`) are the pattern.
- **The Movimientos list is the stack's root** (ADR 022): `AppEntryGraph.kt`'s `HOME_ROUTE` sits at index 0, any other tab root at index 1, and every pushed screen above them, so back always walks down to the list and exits from there. A tab switch is `AppNavigator.selectTab`, which keeps the root entry and swaps only what sits above it. Nothing replaces the root but the manifesto's `replaceAll` on first launch. A screen that must not pop into nothing clears its own state instead of asking for a guard: `AppNavigator.pop()` refuses a stack of one.

## When a new dependency crosses a layer

Before adding a dependency to any package, check the direction above. If the change needs `:core:domain` to reach outward, the design is wrong: invert it with an interface in `:core:domain`.
