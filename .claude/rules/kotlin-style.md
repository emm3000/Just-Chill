---
paths:
  - "androidApp/src/*/kotlin/**"
  - "ui-android/src/*/kotlin/**"
  - "presentation/src/*/kotlin/**"
  - "data/src/*/kotlin/**"
  - "core/domain/src/*/kotlin/**"
---

# Kotlin style rules

Readability is the goal these rules serve. When a rule and readability disagree, say so instead of silently picking one.

## Explicit types

Declare the type on every property and every local `val` / `var`.

| Case | Write |
|---|---|
| Local value | `val remaining: Money = balance.remaining` |
| Property | `private val accounts: StateFlow<List<Account>> = ...` |
| Function return | `fun loadAccount(): Account` — always, including single-expression functions |
| Abstraction matters | `val accounts: List<Account> = mutableListOf()` — declare the supertype, not `MutableList` |

Declare the **supertype** whenever the caller should depend on the abstraction rather than the concrete implementation: `List` over `MutableList`, `Flow` over `MutableStateFlow`, the interface over the implementation.

Omit the explicit type only when it would be pure noise:

- The right-hand side is a constructor call that already names the type: `val insert = TransactionInsert(...)`.
- Delegated properties where the delegate makes the annotation unwieldy: `val vm: AuthViewModel = koinViewModel()` is fine, but do not contort a `by remember` chain to satisfy this rule.
- Lambda parameters whose type is fixed by the receiver and obvious in context.

Primary constructor parameters always carry their type — Kotlin requires it.

## Comments

**Write none.** No KDoc, no `//`, no `/* */`, no section-header banners, no commented-out code.

The code is the explanation. If a line needs a comment to be understood, the fix is a better name or an extracted function with a name that says it — see `naming.md`. A test method's name is its documentation.

Three exceptions survive, and only these. Each is one to three lines of present-tense fact; a surviving comment names a constraint the code cannot show.

1. **Why a non-obvious constraint exists.** The code can show *what* the value is, never *why* it was chosen: the exclusive `>` on `BACKUP_STALE_AFTER_DAYS`, `ñ` folding to `n` in `personKey`. Without it, someone "fixes" the value back.
2. **A warning of consequences.** A workaround, an ordering requirement, a known platform bug, an invariant no test pins.
3. **An external reference.** An ADR, a spec, a migration note the code cannot carry.

A constraint lives once, at the declaration it constrains, never at a call site or on a consuming type. A comment describing behaviour enforced elsewhere is a copy, and copies diverge. Zero history: a sentence about what the code used to be or what a review said is deleted, not rephrased.

Never acceptable: restating what the code does, `// region` headers, `// maps the state`, dead code left commented, or a `TODO` committed without an owner and a reason. A file already in the diff gets its non-surviving comments stripped in the same commit; no repo-wide sweep.

When you delete code, delete it. Git has the history.

## Kotlin idioms

- Prefer `val`. Reach for `var` only when reassignment is the point.
- Prefer extension functions over utility classes and `object` holders.
- Prefer expression bodies for functions that are genuinely one expression, with the return type still declared.
- Prefer `sealed interface` over `enum` when the variants carry data.
- Use `require` / `check` in `init` to reject invalid state at construction — see `principles.md`, fail fast.
- A signature that fits in 120 columns sits on one line: detekt's formatting rules (`ClassSignature`, `FunctionExpressionBody`) redden a hand-wrapped short signature.

## detekt

Config lives in `config/detekt/detekt.yml`, the only source of thresholds; read the file, a copy here goes stale. `./gradlew qualityGate` runs `detektMain` and `detektTest` per module, and on `:androidApp` `detektDevDebug`, `detektDevDebugUnitTest` and `detektProdRelease` instead, and must be green before every commit; plain `./gradlew detekt` covers strictly less and is never the gate. The rules that shape code the most:

- `CyclomaticComplexMethod` (14) and `NestedBlockDepth` (allowedDepth 4, a fifth level fails). Nested `also` / `apply` / `run` / `let` chains get refactored into named intermediate functions or an early return.
- `ReturnCount` (2, labeled returns excluded). More than two real returns means the function should be split.
- `TooManyFunctions` (8 per file) is the rule the repo leans on for Compose decomposition.
- `LongMethod` (60) has no test exclusion: a long test method is a red gate, and so is `MultiLineIfElse` in a test.

### Where detekt goes blind

- `LongMethod` ignores `@Composable` and `@Preview` by config; `LargeClass` counts lines in a class, and a Compose screen is a top-level function. `CyclomaticComplexMethod`, `ComplexCondition`, `NestedBlockDepth` and `LongParameterList` do fire on Composables.
- Type-resolution rules stay silent wherever a symbol does not resolve, and the task still passes. What a module owes is the `compiler errors found during analysis` line its `detekt<Variant>` task prints; `DetektConventionPlugin.putOwnClassesOnAnalysisClasspath` in `build-logic` is what keeps it off, and deleting that wiring deletes analysis silently.
- `ignoreAnnotatedFunctions` takes simple annotation names: `Preview` matches, a fully-qualified name matches nothing and drops the screen into permanent amnesty.
- State hygiene in a ViewModel: `LongMethod` skips `init` blocks and no `onIntent` nears the complexity cap. A green gate is evidence about the Composables, never about the state behind them.

### One baseline file per analysis task

Each analysis task derives its own baseline from the stem `config/detekt/baseline-<module>.xml` set in `DetektConventionPlugin`: `detektMain` fans out into `baseline-<module>-debug.xml` and `-release.xml`, `:androidApp` further over its flavors. The stem files belong to the plain `detekt` task, which is not on the gate: leave them alone.

- To grandfather a pre-existing finding, run the matching baseline task and commit what it writes (`./gradlew :data:detektBaselineMain`). Never hand-edit a baseline, and never baseline a NEW violation the current change introduced.
- A baseline entry for a file-level rule is permanent amnesty: `TooManyFunctions:Foo.kt` carries no count, so the file is exempt at any size. Read the current holders out of the baseline files, never out of a doc.
- Removing an entry or changing detekt config: regenerate into a scratch file and diff against the committed one; a config that silently matches nothing shows up as "no change".

### Burning a baseline down

- A burn-down change only shrinks a baseline. A violation too costly to fix keeps its entry and the commit says why.
- Fix the code, never relax the rule: no threshold edited in `detekt.yml`, no new `@Suppress`.
- `DetektCreateBaselineTask` over-reports relative to `Detekt` (it has flagged `EmptyFunctionBlock` and `TooManyFunctions` the check did not). Never commit a baseline for a source set whose check reports zero findings; delete the file.
- File-level entries are where the effort pays most. `:androidApp`'s `src/dev/.../experiences/` sandbox entries wait on a product call about keeping the sandbox.

### Arbitration and suppression

- Passing the gate is necessary, never sufficient: a reviewer may require a change detekt is happy with.
- A detekt failure becomes a baseline entry only together with a GitHub issue naming it.
- Prefer a baseline entry over an inline `@Suppress`: the baseline is inventoried in one file someone can count; an inline `@Suppress` is invisible and permanent.

### Compose sizing

Function length is a weak signal in Compose: a flat 200-line layout reads fine, a 60-line one with remembered state and nested conditionals does not. The repo measures **decomposition, not length**: one file carrying state, layout *and* N sub-components fails review. Both routes count equally, a component package (`hh/report/components/`, `hh/transaction/sheets/`) or sibling files in the feature package (`hh/profile/`, `hh/recurring/`).

A parameter carrying a default does not count toward coupling: the number that matters is what every caller must supply, which is Compose's own idiom. The flag does not launder a real overage; `LongMethod` and the decomposition rule own what a composable builds out of what it receives.

### Check before committing

1. More than 4 levels of nesting (`NestedBlockDepth`)? Extract a function.
2. A chain of `else if`? Use `when`, or extract functions.
3. A function doing several things? Split it — see `principles.md`, SLAP.
4. Nested `also` / `apply` / `run` / `let`? Refactor into named steps.
