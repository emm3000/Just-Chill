# :feature:transaction — CLAUDE.md

Everything a movement is: `capture/` holds the add, edit and delete form with its sheets and its shortcut combos, `list/` holds the Movimientos tab with its search, its category filter and its pending recurring rows. ViewModels, `UiState` / `Intent` / `Effect`, Compose screens, the three routes and both entry functions live here, in `com.emm.justchill.feature.transaction`.

`id("justchill.android.feature")` plus `kotlinx-coroutines-core`, `kotlinx-datetime`, `androidx-lifecycle-runtime-compose` and `androidx-material-icons-extended`. Depends on `:core:ui` and `:core:domain` and nothing else; `checkModuleBoundaries` fails the gate on any other edge. The transaction row and its `Catalog`, the account, category and date pickers, `AmountInputSheet` and the pending recurring UI are `:core:ui`'s shared vocabulary: consume them, never copy them here.

`./gradlew :feature:transaction:testDebugUnitTest`. The MockK ViewModel suites moved here from `:androidApp` with the ViewModels; `MainDispatcherRule` and `FakeTodayFlow` come from `:core:testing`. `TransactionDateEndToEndTest` stayed in `:androidApp`'s test set: it drives the date through the real repository over an in-memory SQLite, which needs `:core:database`, an edge a feature module may not have.

`id("justchill.screenshot")` renders `AddTransactionScreenContent` over `populatedCaptureState()` under `@PreviewWindowEdges` from `src/screenshotTest/`, against the PNGs in `src/screenshotTestDebug/reference/`. `./gradlew :feature:transaction:validateDebugScreenshotTest` compares, and runs on the gate; `updateDebugScreenshotTest --rerun` re-renders after an intended change and never deletes a stale PNG. The plugin refuses to apply unless the root `gradle.properties` sets `android.experimental.enableScreenshotTest=true`, so AGP logs "The option setting 'android.experimental.enableScreenshotTest=true' is experimental" on every configure; that warning is expected. The renderer skips `AppNavHost`'s `Scaffold` insets and ignores the device spec's `navigation=`, so the `bars` cells subtract a 24dp status bar and a 48dp three-button bar by hand: `360x568 bars` stands in for a 360x640dp phone and `360x728 bars` for the Redmi 15C (360x800dp). Every other cell overstates the pad's height by the system bars.

## Koin and the graph

`transactionModule` is declared here and binds the three ViewModels, nothing else. `:androidApp`'s `wiring/TransactionWiring.kt` includes it and adds the six transaction use cases plus `GetSpendShortcutCombos`, which `ShortcutPublisher` also resolves. `EditTransactionViewModel` is the one parametrised binding: the DSL builds its constructor by hand, so every dependency is listed, `todayFlow` included. Every ViewModel here is listed in `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`.

## Routes and cross-feature navigation

`AddTransactionRoute()` with no preselection is the app's home screen (ADR 017): `:androidApp`'s `AppEntryGraph.kt` names it `HOME_ROUTE` and roots the back stack on it, so the pad's save never pops — `AddTransactionViewModel` empties the amount, the note and the date itself and keeps the resolved defaults. `SeeTransactionRoute` is a plain `AppRoute`, pushed with `pushToTop` by the pad's month-total line and by the "Más" menu row — two doors on purpose. `AddTransactionRoute` and `EditTransactionRoute` are `CaptureRoute`s, the marker `popToCapture()` reads. Opening the menu, adding a category and adding an account all leave this feature, so each arrives as an `(AppNavigator) -> Unit` callback supplied by the host, never as a route value; the category created there comes back through the host's `pendingCategory` accessor, read as a `() -> T` because a `NavEntry.content` closure is cached until the back stack changes.

## Capture

- An id-based selection has no expiry: a preselected id missing when the catalog first loads still lands when a later emission carries it. `selectFrequentCombo` no-ops until the account and category catalogs arrive, and a preselection from a launcher shortcut always arrives before them.
- A preselect is consumed once per ViewModel (`preselectConsumed`). The entry's `LaunchedEffect(key)` restarts on rotation, on a theme change and on popping back from the category screen, and a second firing would silently revert what the user picked.
- A combo can name a deleted account or category: resolve by id, fall back to the normal defaults, never crash and never show an empty selection.
- The amount digits are irreducible. Every entry point shortens the path to the amount pad; no field, chip row or sheet gets added to `AddTransactionScreen`. The month-total line above the hero is a read and a door, never a step: it adds no tap to a capture.
- The month total is `GetMonthSpendUseCase` `flatMapLatest`ed on `todayFlow()`, so a pad left open across midnight re-queries the month it lands in; the label and the amount ride one `MonthSpend` field so they can never name different months.
- The pad is portrait-only (ADR 020): the manifest locks `MainActivity` upright and out of multi-window, and `PadArrangementLayout` picks `StackedTall` whenever the measured tall pad plus the hero's floor (1.3 × `amountHero.fontSize`) fits the height, `StackedShort` otherwise.
- The date is `null` until the save, and `null` is not "no date" — it is the day the movement gets written on. `TodayFlow` decides that day, the injected `Clock` only supplies the time.

## List

- Filter at read time over clearing at write time: a list derived per `transactionType` cannot go stale; a list a reducer must remember to clear always can.
- The browsed month follows a midnight rollover only while it equals the month the rollover leaves. Report's month never moves; `SeeTransactionsViewModelTest` pins both halves.
- Pending recurring movements are about "now": a filtered list stays filtered, and browsing another month never surfaces today's pending row under a month it does not belong to.
- A category filter turns the list into a cross-month search, so the month selector steps aside; in month mode it is always there, including before the ledger count is known.
- `SeeTransactionsViewModel` takes six constructor parameters, the ceiling review holds it to: a datum either needs new joins through the query or an existing flow, not a seventh parameter.
- This screen owns the transaction list and nothing else. Income, spend and balance belong to `:feature:report`; before adding a section, find the owner.
