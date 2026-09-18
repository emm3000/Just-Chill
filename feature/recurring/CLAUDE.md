# :feature:recurring — CLAUDE.md

Recurring movement templates: the list with its monthly totals and its paused section, the add/edit form, the day-of-month sheet and the delete confirmation. ViewModels, `UiState` / `Intent` / `Effect`, Compose screens, routes and `recurringEntries` all live here, in `com.emm.justchill.feature.recurring`.

`id("justchill.android.feature")` plus `kotlinx-coroutines-core`, `kotlinx-datetime`, `androidx-lifecycle-runtime-compose` and `androidx-material-icons-extended`. Depends on `:core:ui` and `:core:domain` and nothing else; `checkModuleBoundaries` fails the gate on any other edge.

`./gradlew :feature:recurring:testDebugUnitTest`. The two MockK ViewModel suites moved here from `:androidApp` with the ViewModels and `RecurringMovementUiTest` from `:presentation`; `MainDispatcherRule` comes from `:core:testing`.

## Koin and the graph

`recurringModule` is declared here and binds the two ViewModels, nothing else. `AddEditRecurringMovementViewModel` takes its `id` as a Koin parameter, so it is a `viewModel { parameters -> ... }` block, not a `viewModelOf`. `:androidApp`'s `wiring/RecurringWiring.kt` includes it and adds the eight recurring use cases; the repositories stay in `:presentation`'s `hh/di/DataModule.kt`, since only the app sees `:core:database`. Both ViewModels are listed in `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`.

## Pendings belong to `:core:ui`, not here

`PendingRecurringUi`, the pending list header and row and `ConfirmRecurringSheet` live in `:core:ui`'s `pending/`, because the Transaction list is what shows a pending (ADR 015, wave 5). Confirm, Skip and their use cases are driven from `SeeTransactionsViewModel` in `:presentation`, which never reaches this module. Moving any of it back here would make the Transaction list depend on the Recurring feature.

## The form

- The category list is cut per type at read time, so an Income template is never offered a Spend category; the composite foreign key the schema holds (ADR 008) is never tested by this screen.
- `Save` writes `selectedCategory` / `selectedAccount`, the **resolved** selection, never the raw id: a category deleted while the form was open is written as "sin categoría" instead of as an id whose row is gone.
- A variable template stores a null amount. `isVariableAmount` and `amountDigits` are separate fields on purpose — toggling variable off restores the digits the user already typed.
- `selectedAccount` missing when `Save` runs is an `error(...)`, not a silent no-op: the CTA is disabled until an account is picked, so reaching it means the screen lied.

## The list

- Active and paused templates are two sorted lists in one state, both ordered by `dayOfMonth` then `name`. A paused template keeps its slot in the day order, it does not drop to the end.
- The monthly totals and the variable count come from `GetRecurringMonthlyTotalsUseCase` over the same emission the lists are built from, never from a second flow; a variable template renders "Variable" and contributes nothing to a total.
- Delete is two steps: `RequestDelete` parks the id in `pendingDelete`, `ConfirmDelete` reads it back. The failure arm clears `pendingDelete` too, so a failed delete does not leave the dialog armed.
