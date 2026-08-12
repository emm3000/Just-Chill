# Code Quality — What We Enforce and Who Enforces It

The convention has exactly two halves, with different owners.

- **Machine half** — what detekt measures. It entered with a baseline, so the gate never goes red on
  code that predates a rule. Owner: `config/detekt/detekt.yml` plus `qualityGate`.
- **Reviewer half** — what no tool can see: whether a module boundary is cut in the right place,
  whether duplication is repeated knowledge or coincidence, whether an abstraction answers a real
  need. Owner fixed by [ADR 007](adr/007-one-way-of-working-writer-reviewer-and-model-tiers.md):
  reviewer always Opus, fresh context, never the writer.

**Admission rule: a rule that does not clearly fall into one of the two halves does not enter**, and
a reviewer-half rule enters only with a test a diff can fail. No third bucket for taste.

## The machine half — thresholds as configured today in `config/detekt/detekt.yml`

| Rule | Threshold | Note |
|---|---|---|
| `LongMethod` | 60 lines | `ignoreAnnotated: ['Composable', 'Preview']` (`:115`) |
| `LargeClass` | 600 lines | measures *classes* |
| `TooManyFunctions` | 8 per file, 11 per class/interface/object/enum | `ignoreAnnotatedFunctions: ['Preview', 'PreviewLightDark']`; test source sets excluded |
| `CyclomaticComplexMethod` | 14 | |
| `ComplexCondition` | 3 | |
| `NestedBlockDepth` | 4 | |
| `ReturnCount` | 2 | `equals` excluded, lambdas excluded |
| `LongParameterList` | 5 function / 6 constructor | data classes ignored |
| `MagicNumber` | active | `-1, 0, 1, 2` allowed; constants, properties and local vals ignored; test source sets *and* `**/*.kts` excluded (`:672`); `ignoreNamedArgument: true` (`:684`); `ignoreAnnotated: ['Composable', 'Preview']` (`:688`) |
| `CognitiveComplexMethod` | **disabled** | |

### What actually reaches a Compose screen — two kinds of blindness, not interchangeable

- **`LongMethod` — blind by config.** `ignoreAnnotated: ['Composable', 'Preview']` at
  `detekt.yml:115`. One line, revertible in one edit; nothing about the rule is Compose-specific.
- **`LargeClass` — blind structurally.** It counts lines in a *class*; a Compose screen is a
  top-level function, so there is no class to measure. No config change fixes that.

`CyclomaticComplexMethod`, `ComplexCondition`, `NestedBlockDepth` and `LongParameterList` have **no
annotation escape and do fire on Composables**. Evidence in
`config/detekt/baseline-ui-android-main.xml`: 3 `CyclomaticComplexMethod` entries
(`AddTransactionScreenContent`, `EmmButton`, `EmmTextInput`) and 24 `LongParameterList`, 22 of them
on a `@Composable` (the two that are not are the `EditTransactionViewModel` and `SyncOrchestrator`
constructors). Live in the source: `@Suppress("CyclomaticComplexMethod")` at
`ui-android/.../ProfileScreen.kt:226`, tracked in `docs/PROGRESS.md`. So `TooManyFunctions` per file
is the rule the repo *leans on* for Compose — not the only one that reaches it.
**What the gate does not cover:** `:ui-android:detektAndroidMainSourceSet` reports **21** issues
deliberately left outside it — `detektMainAndroid` covers the same files *with* type resolution, so
adding it buys tasks, not coverage (`QualityGateConventionPlugin`; item in `docs/PROGRESS.md`).

## Two gotchas, both found by measurement

**1. A baseline entry for a file-level rule is permanent amnesty.** The ID carries no count —
`TooManyFunctions:AccountsScreen.kt:com.emm.justchill.hh.account.AccountsScreen.kt`, and nothing in
that string says "11 functions". The file is exempt at *any* size, forever. **Seven files
hold that amnesty today**, at these non-preview top-level function counts against a threshold of 8:
`SeeTransactionsScreen` 16, `HomeScreen` 16, `AddCategoryScreen` 13, `AccountsScreen` 11,
`AddEditRecurringMovementScreen` 10, `ProfileScreen` 10, `RecurringMovementsScreen` 9. The baseline
stops new bleeding and creates **zero** pressure on old code, so the burn-down list lives in
`docs/PROGRESS.md` — the baseline will never ask.

**2. `ignoreAnnotatedFunctions` takes simple annotation names, not fully-qualified ones.** detekt
matches the name as written in the source: `androidx.compose.ui.tooling.preview.Preview` silently
matches nothing, `Preview` works. Getting this wrong put four extra screens into permanent amnesty.

## Arbitration: when the two halves meet

- **When they disagree.** Passing the gate is *necessary, never sufficient* — a reviewer may require
  a change detekt is perfectly happy with. In the other direction, a detekt failure may become a
  baseline entry **only together with a burn-down line in `docs/PROGRESS.md` naming it**; a silent
  baseline addition is not allowed.
- **How a baseline entry is removed.** It retires when the file stops violating: regenerate the
  baseline into a scratch file, diff it against the committed one, delete what no longer appears.
  Whoever touches that file next does it — at minimum.
- **After changing detekt config.** Same move — regenerate into a scratch file and diff. A config
  that silently matches nothing (gotcha 2) shows up as "no change" and as nothing else: no error, no
  warning, no failing task.

## Suppression policy

- **Prefer a baseline entry over an inline `@Suppress`.** A baseline entry is inventoried in a file
  someone can count; an inline `@Suppress` is invisible and permanent.
- An inline `@Suppress` requires a written justification at the same place, re-checked whenever the
  thing it suppresses changes. Not theoretical: three `@Suppress("TooManyFunctions")` went inert the
  moment one dead method was deleted (`2e7aa81` — the three types dropped 12 → 11 functions), and
  their comments still claimed an overage that no longer existed.

## The acronyms — verdict table

Each **in** row carries the test that makes a finding falsifiable:

| Item | Verdict | Reason and test |
|---|---|---|
| **SOLID S** | **in** | SRP decides where modules cut. **Test:** an SRP finding must name the *two unrelated reasons* the unit would change. If the reviewer cannot name two, it is not a finding. |
| **SOLID D** | **in** | DIP is why `:domain` does not know `:data`. **Test:** mechanical — a DIP finding must point at a forbidden import line or a module dependency that should not exist. No import, no finding. |
| **SOLID O** | weak | Open/Closed is the usual excuse for premature abstraction. |
| **SOLID L** | weak | Almost no inheritance hierarchies here to violate it. |
| **SOLID I** | weak | Across the 9 `:domain` `*Repository` interfaces, **67 method-uses over 57 consumers = 1.18** (measured at `92c6d2cb`; consumers = production classes taking the interface as a constructor parameter; `:data` implementations and Koin modules excluded). **Five of the nine sit at exactly 1.00** — `AccountRepository` dropped off that floor when `EditTransactionViewModel` started using both `all` and `find`; the widest three are now `TransactionRepository` 1.44, `RecurringMovementRepository` 1.33, `AuthRepository` 1.29, and there are **zero role clusters**. Strictly ISP is violated; splitting buys no decoupling. |
| **YAGNI** | **in, and it bites** | Solo developer, no third-party users. Every abstraction built for a future is pure cost. |
| **DRY** | **in — about knowledge, not text** | Two blocks that change for different reasons must not be unified. *Duplication is far cheaper than the wrong abstraction.* |
| **KISS** | **out** | Unfalsifiable. Nobody ever chose the complex option on purpose, so nothing can be reviewed against it. |
| **Design patterns** | not a checklist | A pattern is the name of a solution you arrived at, not a target to hit. |

`:presentation` and `:ui-android` share package names **on purpose**: "deduplicating" across that
boundary collapses it, and same-package symbols still need **explicit imports** because it is a
Gradle module boundary. **The open tension, unresolved:** YAGNI and Clean Architecture pull against
each other — Clean Architecture is speculative generality by design, a port for a database that will
never be swapped, and this repo already paid that bill in the sync layer.

## Compose sizing

Function length is a weak signal in Compose: a flat 200-line layout reads fine; a 60-line one with
remembered state and nested conditionals does not. What correlates with pain is one file carrying
state, layout *and* N sub-components — so the repo measures **decomposition, not length**, and
decomposition happens two ways here, both counting:

- **Dedicated component packages** — `core/ui/atoms/` (20 files, the design system per
  `DESIGN_SYSTEM.md`), `hh/report/components/` (12), `com/emm/justchill/components/` (5 legacy
  `Emm*` widgets, *not* the design system), `hh/transaction/sheets/` (4),
  `hh/transaction/components/` (2). **43 of the 108 `androidMain` Kotlin files** — files in a
  component package over files in that one source set (`androidHostTest` holds the other 3).
- **Sibling files in the feature package** — `hh/profile/` (`DeleteAccountDialog`,
  `ImportBackupDialog`, `RetryPill`, …), `hh/recurring/` (three sheets) and `hh/seetransactions/`
  (`CategoryFilterSheet`, `InfinityScroll`) extract this way, without a `components/` directory.

`hh/shared/` is the cross-feature package, not a feature package — see `ui-android/CLAUDE.md`.

## Use cases

**A use case exists where there is domain logic. A pure read may go from ViewModel to repository.**
The measurement behind that rule:

- **8 ViewModels** in `:presentation` already inject `:domain` repositories directly (measured at `92c6d2cb`; `RecurringMovementsViewModel` joined).
- **5 of 35** use cases in `:domain` are pure delegation, counting strictly: one member whose whole body
  is a single `repository.x(...)` call and nothing else (measured at `92c6d2cb`). `ObserveSessionUseCase`
  (two bare delegations) makes it 6, `SyncDataUseCase` (mutex-wrapped delegation) makes it 7. The 5
  strict survivors — `ClaimLocalDataUseCase`, `SignOutUseCase`, `DeleteCategoryUseCase`,
  `DeleteTransactionUseCase`, `ImportDataUseCase` — are all WRITES, exactly what the rule predicts.
- `FindTransactionUseCase`, `FindAccountUseCase` and `FindCategoryUseCase` were the three worst — eight-line classes renaming `repository.find` — and all three are now deleted (measured at `92c6d2cb`); `CategoryRepository.find` itself had gone dead and was deleted too.

The leak stops at `:presentation`: `:ui-android` and `:androidApp` production code import no
`:domain` repository (only `androidApp/src/test`, which mocks them).
