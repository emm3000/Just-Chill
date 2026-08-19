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
(`AddTransactionScreenContent`, `EmmButton`, `EmmTextInput`) and 23 `LongParameterList`, 22 of them
on a `@Composable` (the one that is not is the `EditTransactionViewModel`
constructor). So `TooManyFunctions` per file is the rule the repo *leans on* for Compose — not the
only one that reaches it.
**What the gate does not cover:** `:ui-android:detektAndroidMainSourceSet` reports a couple of dozen
issues deliberately left outside it — run the task rather than trust a count written here, it moves
with every sweep — `detektMainAndroid` covers the same files *with* type resolution, so
adding it buys tasks, not coverage (`QualityGateConventionPlugin`; item in `docs/PROGRESS.md`).

## Three gotchas, all found by measurement

**1. A baseline entry for a file-level rule is permanent amnesty.** The ID carries no count —
`TooManyFunctions:AccountsScreen.kt:com.emm.justchill.hh.account.AccountsScreen.kt`, and nothing in
that string says "11 functions". The file is exempt at *any* size, forever. **Seven files
hold that amnesty today**, at these non-preview top-level function counts against a threshold of 8:
`SeeTransactionsScreen` 16, `HomeScreen` 16, `AddCategoryScreen` 13, `AccountsScreen` 11,
`AddEditRecurringMovementScreen` 10, `ProfileScreen` 15, `RecurringMovementsScreen` 9. The baseline
stops new bleeding and creates **zero** pressure on old code, so the burn-down list lives in
`docs/PROGRESS.md` — the baseline will never ask.

**2. `ignoreAnnotatedFunctions` takes simple annotation names, not fully-qualified ones.** detekt
matches the name as written in the source: `androidx.compose.ui.tooling.preview.Preview` silently
matches nothing, `Preview` works. Getting this wrong put four extra screens into permanent amnesty.

**3. detekt lints test sources with the production thresholds.** `detektDevDebugUnitTest` analyses
`androidApp/src/test`, and `LongMethod` (60 lines) has no test exclusion — a long test method is a
red gate, not a warning. So is `MultiLineIfElse`: `if (x) a else if (y) b` spread across lines needs
braces in a test too.

## A test that waits must observe the transition, not sample the state

On a `StateFlow`, `flow.first { it }` followed by `flow.first { !it }` can pass while proving
nothing: `first` resolves against the *current* value, so the second call returns immediately if the
work already finished — or if it never started. Conflation can also drop the `true` before a late
subscriber ever sees it. Subscribe before triggering and assert on the recorded sequence, so
"finished" is unreachable without "started".

The check: break the production line so the awaited emission never arrives. The test must fail
naming what did not happen — not hang, and not pass.

No test decides its outcome by how loaded the machine is. `rg 'System.nanoTime|Thread.sleep' --glob
'*Test.kt'` returns nothing, and that is the check — a polled deadline fails the poll rather than the
assertion, so a green run alone and a red run inside a full `qualityGate` are the same code. A real
deadline hidden in a library config counts too: a Ktor `requestTimeout` raced inside `runTest`'s
virtual time is wall clock wearing a different hat.

Where a test genuinely needs real threads, assert the precondition rather than the outcome. A race
that needs two cores proves nothing on one, and no assertion on the result can tell "it did not
happen" from "I could not have seen it".

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

## Comments

**Default is zero comments. A comment exists only to state a constraint the code cannot show — the
why, never the what.**

- **Test (a diff can fail it):** delete the comment mentally. If no *constraint* is lost — only a
  restatement of what the code says — it is noise; delete it for real. If the *what* is unclear
  without it, rename or extract a function instead of commenting.
- Survivors of that test: product decisions invisible in code (the exclusive `>` on
  `BACKUP_STALE_AFTER_DAYS`), warnings about non-obvious consequences, invariants no test pins,
  deliberate duplication markers, and `@Suppress` justifications (the Suppression policy above).
- A kept comment is 1–3 lines of present-tense fact. **Zero history:** a sentence describing what
  the code used to be, what replaced what, or what a review said is deleted — never rephrased.
- **No KDoc by default** — on any class, property, or function, exported to iOS or not. If removing
  the history leaves only a paraphrase of the signature, the whole KDoc goes. **This bullet decides
  EXISTENCE, never form:** a block that survives the deletion test is no longer a default, and the
  bullet below picks its syntax. Read alone, this one has already been misread as "KDoc never".
- **What survives picks its syntax by POSITION, not by length.** KDoc on a declaration something
  else calls, because that is the only form the IDE shows at the call site and the only one whose
  `[Symbol]` links a rename keeps honest; `//` on a statement inside a body, which KDoc cannot
  attach to anyway. The `1–3 lines` ceiling above is unchanged and binds both — KDoc is the form
  that historically slid back into essays, and the ceiling is what stops it, not the syntax.
  **A `@Test` takes `//`:** nothing calls it, so there is no call site to surface, and its name is
  its documentation. This rule describes what `:data` production already does; it is written down
  so a sweep stops re-deciding it per file.
- **Diagrams: one source, maximum.** The backup-row precedence ASCII diagram existed in three
  places; three copies is a divergence liability, not documentation.
- A comment that promises more than the code delivers is worse than none; reviews already caught
  KDocs doing exactly that.
- **A comment describing behaviour enforced elsewhere is a copy, and copies diverge.** A constraint
  lives once, at the declaration it constrains — not at a call site, not on a consuming type, and
  not 85 lines below its own `const val`, which is where a cleanup round left a second wording of
  `MAX_CATCH_UP_MONTHS`'s product limit while the first sat on the declaration. This is the rule the
  three-place precedence diagram broke. If the declaration lacks the constraint and a call site has
  it, move it to the declaration; never keep both.
- **Carrying a true constraint is not sufficient to survive.** True *and* not already stated at the
  declaration. Blocks that fail the second half get deleted whole, not shortened — shortening a
  duplicate leaves a duplicate.
- **Converting KDoc to line comments is not applying this policy.** The test is deletion. A round
  that reformats a comment has decided nothing — `47cd0159`'s sibling nit did exactly that, and the
  comment it preserved turned out to be one of the copies the bullet above forbids.
- **Prefer the rename.** If a comment exists to say what a name should have said, the fix is the
  name: `BackupHealth.isDestinationDisclosed` needed two lines to say `false` blocks every upload,
  and `canUploadToDestination` needs none.

### The ladder — and why "reduced it" is a failure signal

A comment is the LAST resort, and the steps above it are not optional:

1. **Delete** — the comment says what the code does.
2. **Rename** — it says what a name should have said (`isDestinationDisclosed` → `canUploadToDestination`).
3. **Redesign** — it warns about a misuse the design permits (`nowMillis()`'s read-once warning is a
   signature that should take the instant, not a warning).
4. **Comment** — only when none of the above can carry it.

**If you are shortening a comment, you are almost certainly on the wrong step.** Reduction is what
deleting looks like when someone flinched. Three rounds of this policy failed exactly that way —
history rewritten shorter instead of deleted, a rule copied out of the file that enforces it, and a
duplicate of a `const val`'s own constraint kept 85 lines below the declaration.

**Enforcement is the reviewer plus rule 2 above, deliberately — not detekt.** Turning on
`ForbiddenComment` history patterns was tried and abandoned: enabling a detekt rule forces the whole
repo green in one sweep, which turns an incremental policy into a 60-file migration of delegated
judgment calls nobody can review. Worse, a pattern can only see the word, so the way to satisfy it is
to rephrase — the one move this policy forbids. The 448 legacy blocks converge under rule 2 or not at
all, and they sit in code nobody is touching.

### How it spreads — two standing rules

There is no scheduled repo-wide sweep, and the policy does not need one. It applies at two moments,
and a reviewer can fail a diff on either:

1. **New code ships with zero comments.** Any file a unit creates carries no comment and no KDoc
   unless a line passes the survivor test above. This is not a preference — a new comment explaining
   *what* the code does is a naming or decomposition defect, and the fix is the rename or the
   extraction, not the sentence.
2. **Code touched in passing gets stripped.** When a unit edits a file for any reason, its existing
   comments are brought to this policy in the same commit. Not the whole module, not a separate
   cleanup ticket — the file already in the diff. The reviewer's own second half already reads that
   file, so the cost is bounded and the surface converges as the code moves.

**No repo-wide sweep.** Files converge under rule 2 above (touch-it, clean-it) or they do not
converge, and that is the accepted trade: a comment nobody has touched in a year is not the one
misleading anybody.

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
  `hh/transaction/components/` (2). **43 of the 109 `androidMain` Kotlin files** — files in a
  component package over files in that one source set (`androidHostTest` holds another 4).
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
