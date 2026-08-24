# E06-09 — Name the transaction a recurrente creates

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] confirming a recurrente whose description is blank produces a transaction the list identifies
      by the template's name, not by the "Sin descripción" placeholder
- [ ] a recurrente that does carry its own description still uses that description — the name is the
      fallback, never an override
- [ ] a test pins both branches, blank and non-blank
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`ConfirmRecurringMovementUseCase` writes `description = template.description`, and a recurrente's
description is optional while its name is required. Confirming `Netflix` with no description filled
therefore lands a row reading "Sin descripción", so the ledger cannot say what was paid.

Catch-up needs no separate condition: `GetPendingRecurringMovementsUseCase` emits one `PendingRecurring`
per period and each is confirmed through this same use case, so one fix covers every period.

Found on the emulator while verifying E06-03; it predates that ticket and every other in this epic.
