# ADR 008 — The schema owns the category/type invariant

- **Status**: Accepted
- **Date**: 2026-08-12
- **Deciders**: Edgardo Muñoz
- **Amends**: nothing. It states an exception to a rule `data/CLAUDE.md` carried but no ADR had
  decided ("referential integrity is enforced in domain use cases, not by these clauses"), and that
  file is corrected in the same commit.

> Resumen (es): que la categoría de un movimiento sea del mismo tipo que el movimiento deja de ser
> convención y pasa a ser **foreign key compuesta** — `(categoryId, type) → categories(categoryId,
> categoryType)` en `transactions` y en `recurring_movements`. La convención tenía nueve escritores
> que sostenerla a mano y **tres** que ni siquiera pasan por `:domain`, así que cualquier invariante
> puesta ahí tiene techo por diseño: es la razón por la que cuatro rondas de parches en runtime no
> cerraron el bug. La migración `4.sqm` repara antes de reconstruir, y repara **siempre desde el lado
> de la categoría**: el `type` firma el `amount`, así que darlo vuelta reescribiría la historia
> financiera del usuario. El picker acotado por tipo se queda, pero como UX, no como mecanismo.

## Context

A movement (`transactions`, `recurring_movements`) carries a `type` — `Income` or `Spend` — and a
nullable `categoryId`. A category carries its own `categoryType`. Nothing related the two columns:
they were two independent TEXT fields that every writer had to keep in agreement by convention.

The convention did not hold. Creating a category from an Income movement saved it as `Spend` and
filed it under the Income movement anyway, confirmed on the emulator and against the database. The
proximate cause was one navigation default, but the defect class is wider: **nine writers** can put
a `(categoryId, type)` pair into the database, and **three of them never pass through `:domain`** —
`DefaultBackupRepository` (backup import), `TransactionTableSync` and `RecurringMovementTableSync`
(the sync pull). A fourth, `CategoryTableSync`, can change a category's type underneath movements
already filed under it.

Four rounds of runtime patches were written against this and none closed it. That is not four
mistakes; it is the ceiling of the approach. An invariant in `:domain` cannot reach a writer that
does not call `:domain`, and the discarded attempt also introduced a CRITICAL of its own: a
`NotFound` raised on a tombstoned category made recurring templates permanently unconfirmable. Those
patches are kept only on the tag `patches-descartados-2026-08-12`.

Two facts about the data make "just fix it forward" insufficient. The author runs the release build
daily on a device holding real accumulated data, so mismatched rows already exist there. And every
backup file on disk predates any of this, so the one safety net is full of pairs the new rule
rejects.

## Decision

1. **The database enforces it.** `transactions` and `recurring_movements` declare a composite
   foreign key `(categoryId, type) → categories(categoryId, categoryType)`. `categories` gains the
   UNIQUE index over `(categoryId, categoryType)` that SQLite requires over the parent columns —
   without it the child DDL parses and every insert then fails at runtime with "foreign key
   mismatch", which is a different exception type from a constraint violation and one no caller
   catches.
2. **`CategoryType` and `TransactionType` stay separate enums.** `TransactionType` already declares
   `val categoryType: CategoryType` and the mapping is total. Their `label`s differ on purpose and
   are user-facing (`"Ingresos"` vs `"Ingreso"`); collapsing them is churn that breaks UI copy. What
   IS load-bearing and invisible to the compiler is that the two enums share CONSTANT NAMES — the
   key compares the columns as text.
3. **Neither key carries an `ON DELETE` clause.** `ON DELETE SET NULL` on a composite key nulls
   every child column, `type` included, and `type` is `NOT NULL`, so the clause could only ever
   abort the delete. Nothing is lost: no code path physically deletes a category — `DeleteCategoryUseCase`
   tombstones, and preserves the dangling link on purpose — so the clause never fired.
4. **The migration repairs from the category side, never the type side.** `type` signs the amount
   (`getAccountBalance`, `liveTotals`), so rewriting a movement's type to resolve a mismatch would
   move the user's balance. Nulling the category loses a label the user can restore in two taps;
   flipping the type loses the truth.
5. **Repair does not dirty the rows it touches** — no `updatedAt` bump, no `syncState = 'Pending'`.
   Under [ADR 006](006-sync-is-backup-only-one-device-at-a-time.md) there is no second replica for a
   correction to reach; all it would buy is a first backup diff inflated by every repaired row.
6. **The two `:data` entry points repair rather than fail**, with one deliberate asymmetry. Backup
   import nulls the category on any pair the key would refuse, absent or mismatched alike, because
   the whole restore is one transaction and a raw violation costs the entire import. The sync pull
   keeps them apart: absent still defers on the FK (the parent may yet arrive), mismatched writes
   the row uncategorized and logs why (categories are pulled before movements in the same cycle, so
   the parent is already current and deferring would hold the shared cursor forever).
7. **Scoping the pickers by type is UX, not the mechanism.** The forms only ever offer categories of
   the movement's type and the new-category route carries that type, so a user cannot reach the
   constraint. That is worth having — the alternative is a generic database error with nothing on
   screen explaining it — but it is not what makes the invariant true.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Enforce it in a `:domain` use case / runtime invariant** | Cannot reach the three `:data` writers that never call `:domain`, which is precisely where the corruption entered. Tried, reviewed, discarded; it also broke recurring confirmation on tombstoned categories. |
| **Collapse `CategoryType` into `TransactionType`** | Removes the mismatch by removing one of the types, but their labels are different user-facing copy, and it still leaves two unrelated TEXT columns in the database. Churn without enforcement. |
| **Fix only the navigation default that produced the bug** | Closes one of nine writers. The other eight, and the rows already on the device, stay exactly as they were. |
| **Repair by flipping the movement's type to match its category** | Silently rewrites financial history: the balance aggregates sign `amount` by `type`. Irreversible and invisible. |
| **Drop the mismatched rows in the migration** | Deletes movements the user actually recorded, to fix a label. |
| **A CHECK constraint or a trigger** | SQLite `CHECK` cannot reference another table. A trigger could, but it duplicates what a foreign key already means, is not covered by the migration verifier, and has no equivalent of the FK-miss deferral the sync pull depends on. |

## Consequences

### Positive
- The invariant holds for every writer, present and future, including ones written by someone who
  never read this file. That is the entire point.
- `DomainException.DatabaseError` is the worst case, not silent corruption.
- The migration is also a data repair: existing mismatched and orphaned pairs are cleaned on the
  first open, on both platforms.

### Negative / costs
- **A category can no longer serve both sides of the ledger.** It never really could — `categoryType`
  is a single column — but a test asserted in a comment that it was normal, and it was seeded that
  way. A user wanting "Préstamos" in and out needs two categories.
- The two enums' constant names are now a schema contract with no compiler check behind it. Renaming
  `TransactionType.Income` or `CategoryType.Income` breaks every write in the app; only the header
  comment in `transactions.sq` says so.
- `4.sqm` rebuilds two tables. It is the second destructive migration here, and the instrumented
  suite is the only thing that runs it against a real driver.
- One more freeze mode had to be closed on the way: a remote category changing its type would strand
  the movements filed under it, so `CategoryTableSync` now detaches them before updating the parent.
  Sync is off in production, so this is dormant machinery — it is fixed because the change created it.

## Notes

- Mechanics and the ordering constraint (repair before rebuild, because iOS migrates with foreign
  keys enabled): the header of `4.sqm` and `data/CLAUDE.md`.
- Discarded runtime-patch attempts: tag `patches-descartados-2026-08-12`. Do not resurrect them.
- [ADR 001](001-reverse-local-only-to-local-first-optional-sync.md)'s Consequences describe the
  `categoryId` foreign key as `ON DELETE SET NULL`. That clause is gone as of this ADR; 001's own
  decision — that soft-delete integrity is reimplemented in use cases — is untouched, and 001 is
  left as written, per the rule that ADRs are amended by a new ADR and never rewritten.
