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

- **`sendEffect` and `updateState` run on `viewModelScope`, and so does every repository call.**
  No ViewModel switches dispatcher (`rg 'withContext|Dispatchers\.' presentation/src/main/kotlin/com/emm/justchill/hh`
  is empty); `:data` owns its own threading. A `withContext(IO)` in a ViewModel is a second
  threading policy, not an optimisation.
