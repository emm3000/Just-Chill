# E10 — ViewModel state hygiene

**Follows:** the 2026-08-26 audit of `AddTransactionViewModel`, which found seven mutable instance
fields living outside the `StateFlow` and one user-visible bug that fell out of them.

## Why

Ten of the sixteen ViewModels in `:presentation` hold zero mutable fields. Three hold three or more,
and all three are transaction-shaped forms that resolve `Account`/`SelectableCategory` once, cache
the result by hand, and then have to remember to keep it in sync. Every defect the audit found comes
out of that one habit. This track removes the habit, not the symptoms.

## Constraints

- **A UiState stores what the user chose, never what was resolved.** What the user chose is an id;
  the resolved object is a getter derived from the catalog held in the same state. A resolved object
  stored in a field is a cache with no invalidation — it is how a selection outlives the row it
  points at and a movement gets filed under a deleted category with no error.

- **Filtering at read time beats clearing at write time.** A list the state derives per
  `transactionType` cannot go stale; a list a reducer has to remember to clear always can. Prefer the
  getter to the extra line in the `copy`.

- **detekt cannot see this class of defect in a ViewModel, and it never will.** Three measured
  reasons: `LongMethod` does not inspect `init` blocks — `SeeTransactionsViewModel`'s is 83 lines and
  green with no `@Suppress` and no baseline entry — `CognitiveComplexMethod` is off in
  `config/detekt/detekt.yml`, and no `onIntent` here comes near `CyclomaticComplexMethod`'s 14: the
  largest `when` in the repo scores about 11. That rule does bite one layer up — E10-01 pushed
  `AddTransactionScreenContent` to 16 with a three-arm `when` at a call site — so read a green gate as
  evidence about the Composables, never about the state behind them.

- **`AuthViewModel` is the reference, and it is already in this repo.** Nine intents, four use cases,
  zero mutable fields: the re-entrancy guard lives in the sealed state and `launchSubmitting`'s
  `finally` clears it on success, domain failure and cancellation alike. Copy that shape before
  inventing another one.

- **`MviViewModel` is not the problem.** Every fix in this track lands through the existing
  `updateState`. A ticket that reaches for the base class has misdiagnosed something.

- **A collection reaching a UiState must be immutable.** `:ui-android` declares this module's state
  classes stable on its own side (`ui-android/CLAUDE.md`); a `MutableMap` or a `var` in the state
  turns that declaration into a lie no compiler can catch.

- **`TodayFlow.today()` exists so no consumer derives the date a second way.** Its KDoc says exactly
  that. A hand-written `private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date` in a
  ViewModel is that second way; there are four of them.
