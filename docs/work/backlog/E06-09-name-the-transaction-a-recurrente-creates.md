# E06-09 — Name the transaction a recurrente creates

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] confirming a recurrente whose description is blank produces a transaction the list identifies
      by the template's name, not by the "Sin descripción" placeholder
- [ ] a recurrente that does carry its own description still uses that description — the name is the
      fallback, never an override
- [ ] the same holds on the catch-up path, where several months are settled at once
- [ ] a test pins both branches, blank and non-blank
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`ConfirmRecurringMovementUseCase` writes `description = template.description`, and a recurrente's
description is optional while its name is required. Confirming `Netflix` with no description filled
therefore lands a row reading "Sin descripción", so the ledger cannot say what was paid.

Found on the emulator while verifying E06-03; it predates that ticket and every other in this epic —
the flow only moved screens, it did not change.
