# E12-01 — Make `initialState` a constructor argument

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** —

## Done when

- [ ] `MviViewModel` takes `initialState` in its constructor as a `protected val`;
      `rg 'by lazy' presentation/src/main/kotlin/com/emm/justchill/core/mvi` returns nothing.
- [ ] `rg 'override val initialState' presentation/src ui-android/src androidApp/src` returns
      nothing — the sixteen ViewModels and `MviViewModelTest`'s `LaunchSafeViewModel` pass it to
      `super`.
- [ ] `ReportViewModel` and `SeeTransactionsViewModel` seed their opening month from
      `todayFlow.today()` — the expression their `stateIn` seeds already use — because a constructor
      argument cannot read an instance property.
- [ ] The ordering comment in `AddTransactionViewModel.init` ("must stay declared above this
      block") is gone: `rg -i 'lazy' presentation/src/main/kotlin/com/emm/justchill/hh` returns nothing.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

Both `by lazy` delegates exist only so the base constructor does not read an abstract property. The
price is an invariant held by declaration order in sixteen files and one comment: reorder two lines
and the first state access is an NPE at runtime. `SeeTransactionsViewModel` derives
`selectedMonth`/`calendarMonth` from `initialState`, hence the `protected val`.
