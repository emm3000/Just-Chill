# :feature:account — CLAUDE.md

Accounts: the month list, create, edit, delete, and the Loan balance row that sits under them. ViewModels, state and Compose screens together in `com.emm.justchill.feature.account`.

`justchill.android.feature`, which brings `:core:domain`, `:core:ui`, Koin, lifecycle, navigation3-runtime, the serialization plugin and `:core:testing`. The build file adds one library of its own, `androidx-material-icons-extended`, for the account type glyphs.

## DI and navigation

- `accountModule` exposes the two ViewModels and nothing else. `:androidApp`'s `wiring/AccountWiring.kt` `includes` it and binds the three use cases, because a use case is not the feature's to own.
- `AccountRoutes.kt` holds `AccountsRoute` and `AddAccountRoute`, both `@Serializable`, plus `accountRoutes`, the registry `:androidApp`'s `RouteSerializationTest` unions. A new route lands in both or it is never round-tripped.
- `accountEntries` takes `onOpenLoans: (AppNavigator) -> Unit`; `:androidApp` supplies the push, since the Loans feature is not this module's to import. `LoansSection` is the only caller, and it is mounted even with an empty ledger: gating it on the ledger having data leaves the ledger unreachable.

## Feature gotchas

- No screen shows an account balance. Accounts have no opening balance, so `AccountMonthUi.net` is a month-scoped net and the row says so.
- An account carries no colour: its row shows the type icon in grey (ADR 017). `AccountPalette.kt` here holds the type icon and label maps and nothing else.
- Delete refuses while the account still has live transactions (`AccountHasTransactions`); the confirm dialog warns about them.
- `AccountsViewModel` takes six constructor parameters, the ceiling review holds it to: a new datum needs a join through the query or an existing flow, never a seventh parameter.
- The month comes from the injected `TodayFlow`, never a hand-written `today()`.

## Testing

`./gradlew :feature:account:testDebugUnitTest`. `AccountsViewModelTest` uses MockK with `MainDispatcherRule` and `FakeTodayFlow` from `:core:testing`; `AccountRowToneTest`, `LoansSectionToneTest` and `AccountsCopyTest` are plain `kotlin.test` over the pure functions next to the screens.
