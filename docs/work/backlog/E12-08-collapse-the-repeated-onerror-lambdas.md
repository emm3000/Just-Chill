# E12-08 — Collapse the repeated `onError` lambdas

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** E12-04

## Done when

- [ ] `SeeTransactionsViewModel`, `ProfileViewModel` and `AccountsViewModel` each hold one
      `private val onDomainError: (DomainException) -> XEffect` (or equivalent), and every
      `launchSafe`/`launchSafeIn` call whose reaction is that class's plain `ShowError`/`ShowMessage`
      references it instead of re-writing the lambda.
- [ ] A site whose reaction genuinely differs (`ProfileViewModel`'s branch-by-exception-type import
      handler, its unrelated `Notify` case) stays a lambda of its own — collapsing only removes
      duplicated knowledge, never a real branch.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

The same `onError` shape repeats across most of each class's `launchSafe`/`launchSafeIn` call sites —
one piece of knowledge per class, not coincidence (`docs/CODE_QUALITY.md`'s DRY-over-knowledge row).
Blocked on E12-04: it is rewriting `SeeTransactionsViewModel` right now and would conflict.
