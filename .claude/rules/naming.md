---
paths:
  - "androidApp/src/*/kotlin/**"
  - "ui-android/src/*/kotlin/**"
  - "presentation/src/*/kotlin/**"
  - "data/src/*/kotlin/**"
  - "domain/src/*/kotlin/**"
---

# Naming rules

Three combined sources: Uncle Bob (Clean Code), official Kotlin, and professional Android practice. On conflict, that is the priority order.

## Language

- **English for every identifier**, including test fixtures. Spanish appears only in user-data VALUES: `name = "Sueldo"` is data; `val sueldo` is a violation. Name fixture locals by role (`incomeCategory`, `cashAccount`).
- User-facing Spanish addresses the reader as **tú, never vos**. A voseo string is a defect even when it reads well, and a test can pin the wrong one: grep the expectation, not just the source.

## Uncle Bob, applied to Kotlin/Android

| Rule | Bad | Good |
|---|---|---|
| Name reveals intent | `d`, `data`, `tmp` | `accountId`, `filteredTransactions`, `elapsedMs` |
| No disinformation | `transactionList` (it's a `List`) | `transactions` |
| Meaningful distinction | `getLoan` vs `fetchLoan` vs `retrieveLoan` | one verb per concept |
| Pronounceable | `genDtTmStmp` | `generatedAt` |
| Searchable (no magic literals) | `if (days > 3)` | `if (days > BACKUP_STALE_AFTER_DAYS)` |
| Classes: nouns | `DataProcessor`, `Manager` | `LoanRepository`, `AuthViewModel` |
| Functions: verbs | `loan()`, `data()` | `loadLoan()`, `buildState()` |
| One word per concept | `fetch` in one place, `get` in another | pick one and use it across the codebase |
| No humor or jargon | `whack()`, `eatMyShorts()` | `delete()`, `clear()` |

A name that needs a comment to be understood is the wrong name. Rename it instead of explaining it — see `kotlin-style.md`, comments.

## Official Kotlin

- **Classes / objects / interfaces**: `PascalCase` — `AuthViewModel`, `UiState`
- **Functions / properties**: `camelCase` — `loadLoan()`, `isLoading`
- **Constants** (`const val`, companion, top-level): `SCREAMING_SNAKE_CASE` — `RESEND_COOLDOWN_MS`
- **Packages**: `lowercase.nounderscores`
- **Backing properties**: `_` prefix + same name — `_state` / `state`
- **Lambdas**: use `it` only if the context is obvious in 2 lines or fewer; otherwise name the parameter
- Prefer `val` over `var`; prefer extension functions over utility classes

## Patterns by layer

| Type | Pattern | Examples |
|---|---|---|
| Domain model | `{Entity}`, no suffix, in `domain/.../<entity>/` | `Loan`, `Transaction` |
| Insert / update payload | `{Entity}Insert`, `{Entity}Update` | `TransactionInsert`, `LoanUpdate` |
| Identifier | value class `{Entity}Id` in `shared/EntityIds.kt` | `AccountId`, `LoanId` |
| UseCase | `<Verb><Subject>UseCase`, `operator fun invoke` | `CreateLoanUseCase`, `GetSavingsRateUseCase` |
| Repository (interface) | `{Entity}Repository` in `:domain` | `LoanRepository` |
| Repository (impl) | `Default{Entity}Repository` in `:data` | `DefaultLoanRepository` |
| Local model / source / mappers | `{Entity}Entity`, `{Entity}LocalDataSource`, `{entity}Mappers.kt` | `LoanLocalDataSource` |
| ViewModel | `<Feature>ViewModel` | `AuthViewModel` |
| UiState | `<Feature>UiState` — data class, all fields `val` | `AuthUiState` |
| Intent | `<Feature>Intent` — sealed interface, past tense or noun-verb | `EmailChanged`, `Submit`, `GoogleSignInClicked` |
| Effect | `<Feature>Effect` — sealed interface, describes the effect | `NavigateBack`, `ShowError` |
| Presentation model | `<Thing>Ui` with a `toUi` mapper | `TransactionUi`, `PersonBalanceUi` |
| Koin module | `<feature>Module` in `hh/di/<Feature>Module.kt` | `authModule` |
| Screen / entries / route | `<Feature>Screen`, `<Feature>Entries.kt`, `<Feature>Route` | `AuthScreen`, `authEntries`, `AuthRoute` |
| Atom | role name, file matches, under `core/ui/atoms/` | `IconBtn`, `Pill`, `StickyCTA` |
| Exposed Flow/StateFlow | name without the `Flow` suffix | `val accounts: Flow<List<Account>>`, not `accountsFlow` |
| Booleans | `is`, `has`, `can`, `should` prefix | `isLoading`, `hasLoans`, `canResend` |
| Callbacks in a Composable | `on` prefix | `onIntent`, `onBack`, `onDismiss` |
| Suspend fun | name it as if it were synchronous | `fetchById()`, not `fetchByIdSuspend()` |
| Test method | backtick sentence naming the rule | `` `refuses while live dependents exist`() `` |

## Additional rules

- `Intent` names describe **what the user did**, not what the ViewModel should do: `DeleteClicked`, not `TriggerDelete`.
- `Effect` describes **the resulting effect**, not the action: `NavigateBack`, not `GoBack`.
- A private ViewModel function takes the `handle` prefix only when it groups several sub-cases. If it does one thing, name it directly: `loadLoan()`, not `handleLoadLoan()`.
- Avoid redundant prefixes inside a scope: inside `LoanDetailViewModel`, `loadLoan()`, not `loadLoanDetail()`.
- A test expectation is pinned to the rule, not to the current output.
