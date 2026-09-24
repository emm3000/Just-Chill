---
paths:
  - "**/src/*/kotlin/**"
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
- A signature that fits in 120 columns sits on one line; detekt's formatting rules rewrite a hand-wrapped short one.

## Complexity limits

detekt enforces them on the gate (ADR 018). The numbers live in `config/detekt/detekt.yml` and nowhere else: nesting depth, real returns per function (a labeled return out of a lambda is not one), functions per file, cyclomatic complexity, and function length, which holds in tests too. Nested `also` / `apply` / `run` / `let` chains get refactored into named intermediate functions or an early return. `@Composable` and `@Preview` functions are exempt from the length limit — see Compose sizing below, where decomposition is the measure instead.

A finding is fixed in code or its rule is changed in that file; there is no baseline. Passing the gate is necessary, never sufficient: a reviewer may require a change no rule forbids.

### Compose sizing

Function length is a weak signal in Compose: a flat 200-line layout reads fine, a 60-line one with remembered state and nested conditionals does not. The repo measures **decomposition, not length**: one file carrying state, layout *and* N sub-components fails review. Both routes count equally, a component package (`feature/report/components/`, `feature/transaction/capture/sheets/`) or sibling files in the feature package (`feature/profile/`, `feature/recurring/`).

A parameter carrying a default does not count toward coupling: the number that matters is what every caller must supply, which is Compose's own idiom. The flag does not launder a real overage; the length and decomposition limits own what a composable builds out of what it receives.

### Check before committing

1. Too deep to read? Extract a function.
2. A chain of `else if`? Use `when`, or extract functions.
3. A function doing several things? Split it — see `principles.md`, SLAP.
4. Nested `also` / `apply` / `run` / `let`? Refactor into named steps.
