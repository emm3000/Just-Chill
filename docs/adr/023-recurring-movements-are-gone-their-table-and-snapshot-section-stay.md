---
status: accepted
date: 2026-09-25
---
# Recurring movements are gone; their table and snapshot section stay

The owner asked for the feature to go, on the same day ADR 022 put the list
back as home: "la funcionalidad donde programas autocobros se va al agua".
Recurring movements were a template per month that the owner had to confirm
or skip by hand, so they saved no typing and added a "Pendientes" sheet, a
list in Más, a form with day-of-month and frequency sheets, and a monthly
summary nobody read. The PRD never named them; CLAUDE.md's Product line and
CONTEXT.md did.

## Decision

- **The feature surface goes.** `:feature:recurring` is deleted with its
  routes, entries, module and tests; `RecurringWiring.kt` and its line in
  `AppGraph.kt` go; the `Recurrentes` row leaves Más; the pending-recurring
  state, intents, sheet and rows leave `SeeTransactions*`; the domain loses
  every recurring use case, model, rule and the `RecurringMovementRepository`
  interface, and `:core:database` loses that repository's implementation
  and its mappers.
- **The table stays.** `recurring_movements.sq` keeps its `CREATE TABLE`
  and `4.sqm` keeps rebuilding it, because dropping a table is a destructive
  migration on the device that holds the author's real data, and the
  schema's history is not rewritten. The `.sq` loses its queries; no
  `.sqm` and no `databases/N.db` ship, because the schema does not change.
- **The snapshot section stays.** `ExportPayloadDto.recurringMovements`,
  its DTO, `BACKUP_RECURRING_SINCE_VERSION` and the `BackupV*` frozen types
  are untouched, so every snapshot already in Storage still decodes and
  restores. `SnapshotStore` keeps writing the section from the table and
  restoring it into the table; from now on it carries the rows the device
  had on 2026-09-25, and no screen reads them.
- **Docs.** CLAUDE.md's Product line and module list, CONTEXT.md's
  `### Recurring` block (its terms become `_Avoid_` under Transaction, so
  "period" and "pending" are not reused with another meaning), and
  `docs/PRODUCT_REQUIREMENTS.md` gain row W-13 for this ADR.

## Consequences

`RecurringMovementFkTest` and the migration tests that seed the table stay:
they guard `4.sqm`, which still runs on a fresh install. `core/database/CLAUDE.md`'s
restore-order note ("the recurring templates go last") stays true. The
launcher shortcut set does not change. The apk loses one feature module and
the Koin graph one wiring file. Anyone who later wants scheduled movements
starts from a PRD row that reverses W-13, not from this code.

## Considered options

- **Drop the table with a migration.** Cleaner schema, one destructive
  migration on real data for a table no one reads; refused by the house rule.
- **Keep the feature and hide the entry point.** Dead code kept warm, the
  pending sheet still computed on every list load, the test matrix still
  paid for.
- **Drop the snapshot section too.** Every existing snapshot becomes
  unreadable on the new build, and ADR 009's restore drill fails on the
  owner's own backups.
