# E12 — MVI core

**Follows:** the 2026-08-27 audit of `MviViewModel` and its sixteen subclasses. Verdict: keep it —
the base class is small, every screen honours its contract, and its one exception policy is tested.
The defects are in what the base class does not offer and in one workaround it carries.

## Why

E10 wrote "`MviViewModel` is not the problem" and it holds for state hygiene: every E10 fix landed
through `updateState`. This track is the base class itself — a `by lazy` that exists only to dodge
an abstract-property-in-constructor read, an error funnel with a door on the suspend side and none
on the flow side, and a `SavedStateHandle` decision nobody has made. Google's canonical guidance
(`developer.android.com/topic/architecture/ui-layer/events`) reduces one-off events to state; the
`Channel` pipe here is the mainstream alternative, and the constraints below are what keep it safe.

## Constraints

- **A screen reaches its ViewModel through `state`, `effect` and `onIntent`, nothing else.**
  `rg 'vm\.\w+' ui-android/src/main` hits only those three today. A public helper on a ViewModel
  called from a Composable bypasses the intent contract and is invisible to every test that drives
  the ViewModel through `onIntent`.

- **`launchSafe` rethrows `CancellationException` before its broad catch, and `MviViewModelTest`
  pins it.** `ReportViewModel` keeps latest-wins jobs and cancels the in-flight one on every filter
  change; a funnel that read cancellation as failure would snackbar on every re-query.

- **Effects ride a `Channel`, not a `SharedFlow`.** Every collector is a `LaunchedEffect(vm)`
  inside the screen's `entry`, so it dies with the composition when the entry is buried in the
  back stack. The channel buffers what is sent while nobody listens and hands it to the next
  collector; a replay-0 `SharedFlow` drops it on the floor.

- **An effect is a navigation or a transient message.** Anything the UI keeps rendering — a sheet,
  a dialog, a focus — is state, because a one-shot event into a `remember` flag cannot be restored,
  cannot be tested through `state.value`, and disagrees with the ViewModel the moment either side
  misses a beat.

- **A sheet, a dialog or any visibility flag the UI keeps rendering lives in `UiState`, never in
  `remember` or `rememberSaveable`**
  ([ADR 012](../../adr/012-typed-form-input-dies-with-the-process.md)). The fields inside it live in
  the ViewModel, so they survive a configuration change and die with the process: `rememberSaveable`
  outlives them and `remember` dies before them. Only `UiState` matches, and a mismatch ships as a
  restored sheet over empty fields.

- **`sendEffect` and `updateState` run on `viewModelScope`, and so does every repository call.**
  No ViewModel switches dispatcher (`rg 'withContext|Dispatchers\.' presentation/src/main/kotlin/com/emm/justchill/hh`
  is empty); `:data` owns its own threading. A `withContext(IO)` in a ViewModel is a second
  threading policy, not an optimisation.

- **A funnelled collector survives three failures, then dies for the ViewModel's life.**
  `launchSafeIn` re-subscribes up to three times with exponential backoff (200/400/800 ms) and only
  then lets the error through, so a failure episode is one snackbar however many attempts it took.
  That heals what actually reaches it — SQLite lock contention — without the user ever knowing, and
  it rests on SQLDelight's `Query.asFlow()` being a cold flow that re-runs its query per collector.
  What it does not heal is a failure that persists: the catch is still *outside* `collect()`, so the
  fourth one ends that flow for good. `AccountsViewModel` and `SeeTransactionsViewModel` outlive
  `switchTab`'s backstack rewrite as the same instance — a bottom-bar tab you leave and return to
  never gets a fresh collector — so a database that stays broken still leaves a frozen list with no
  route back short of process death. Every other `launchSafeIn` site pays the same, for as long as
  its own instance lives.

- **`stateIn`/`shareIn` upstreams bypass the funnel entirely.** Today the only two sites are
  `ReportViewModel.calendarMonth` and `SeeTransactionsViewModel.today`, both fed by `todayFlow()`,
  whose chain is `Clock`-only with no repository in it — so nothing can raise a `DomainException`
  there. The day anything data-backed gets `stateIn`'d, that error has no door.

- **One sheet-visibility field per screen: an enum when the screen has more than one mutually
  exclusive sheet, a `Boolean` when it has exactly one.** Every sheet is a `ModalBottomSheet`, so
  two cannot be open at once; four booleans spell sixteen states of which five are legal, and the
  enum makes the rest unrepresentable rather than merely unreached. `SeeTransactionsUiState` is the
  one screen carrying both shapes, because its confirm sheet is keyed by an id.

- **A `when` helper split out of `onIntent` takes a nested sealed sub-interface, never the wide
  intent type with an `else`.** `LoanDetailViewModel.onPaymentFormIntent` and
  `SeeTransactionsViewModel.onScreenChromeIntent` are the two sites, and detekt's
  `CyclomaticComplexMethod` ceiling of 14 is what forces the split — `SeeTransactions` reached 17.
  The wide-typed version clears that ceiling just as well while silently swallowing any label added
  to `onIntent` and forgotten in the helper.
