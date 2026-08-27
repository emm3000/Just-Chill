# Persistence — the schema, its migrations, and the tests that prove them

SQLDelight 2.x on device is the single source of truth. Everything here lives in `:data`; the module's
own conventions are in `data/CLAUDE.md`.

## Schema

- Schema and migrations both live in `data/src/main/sqldelight/com/emm/data/` — read the
  directory, never a list written down somewhere. `.sq` files carry the CREATE TABLE plus that table's
  queries (`backup.sq` is the exception: no table of its own, only cross-table reads). Migrations are
  `0.sqm`…`5.sqm`, so the current schema is **v6**. **Both kinds move freely but never rename** — E11
  moved the whole directory out of `commonMain` and touched no name. A `.sqm`'s digit is the version
  it migrates from, mirrored by every migration test's `oldVersion`; a `.sq`'s name becomes its
  generated `<Name>Queries` class.
- Generated database class: `EmmDatabaseData` (package `com.emm.data`), configured in
  `data/build.gradle.kts`.
- **`transactions.occurredAt` is ISO local text, not an instant** — `'2026-08-10T21:47:33'`, no
  timezone. Ordering, month windows and day grouping are all plain string operations on it; see the
  header comment in `transactions.sq` for why that works: an instant column made "what day is
this" depend on the reader's zone, which is the bug the text column removes.
  `createdAt` / `updatedAt` / `deletedAt` stay epoch millis: those are genuine instants.
- **Soft-delete (tombstones)** since schema v3: deletes are `UPDATE ... SET deletedAt,
  syncState='Pending'`; every read query filters `deletedAt IS NULL`. Sync metadata columns on
  every table: `userId` (nullable), `deletedAt` (nullable epoch ms), `syncState` (default `'Pending'`).
- **The category/type relation IS enforced by the schema.** Since v5 `transactions` and
  `recurring_movements` declare a COMPOSITE foreign key, `(categoryId, type) → categories(categoryId,
  categoryType)`, so a movement can never carry a category of the other type. It lives here and not in
  a use case because `DefaultBackupRepository` (import) reaches the database without passing through
  `:domain`. Full reasoning in the header of `transactions.sq`; the decision and the rejected
  alternatives in [ADR 008](adr/008-the-schema-owns-the-category-type-invariant.md).
  Two properties of it that surprise people: a composite FK with any NULL column is SATISFIED, so an
  uncategorized movement needs no special case; and it carries **no `ON DELETE` clause**, because
  `SET NULL` on a composite key would try to null `type`, which is `NOT NULL`.
- The remaining FK clauses (`transactions.accountId → accounts ON DELETE RESTRICT`, same on
  `recurring_movements`) **only fire on physical DELETE — never on soft-delete**. Deletion integrity
  is enforced in domain use cases (`DeleteAccountUseCase`, `DeleteCategoryUseCase`), not by them.

## Migrations are mandatory

Never edit a `.sq` CREATE TABLE without a matching `.sqm`, and never reset the schema. There IS real
data to lose: no third-party users, but the author runs the release build daily off Firebase App
Distribution, and every push to trunk distributes to that device. The rule also stands on its own — a
missing migration is the one defect this repo cannot test its way out of, because it only fires when
an already-installed app opens a newer schema.

The schema snapshot lives in `data/src/main/sqldelight/databases/`; regenerate with
`./gradlew :data:generateDebugEmmDatabaseDataSchema` when bumping the version.
`./gradlew :data:verifySqlDelightMigration` replays the `.sqm` files over that snapshot and fails if
the result differs from the `.sq` CREATE statements. It is on `check` (SQLDelight wires it there) **and
on `qualityGate`** — the second one is what matters, since nothing here runs `check`.

**A schema bump also rehearses the restore**, and it is a different question: the suite proves a
migration preserves rows already on the device, never that a snapshot written *before* the bump can
still be read *after* it. Before shipping one, import the latest production snapshot onto a clean
emulator and compare `ImportStats` — `accounts`, `categories`, `transactions`, `recurring`, `loans`,
`loanPayments` — against the pre-bump counts. Any of the six that moved is a failure. Restoring is the
only path back from a migration that loses data, so a bump that has not been restored from is untested.

## The instrumented suite

Migration tests live in `data/src/androidTest/` and run with
`./gradlew :data:connectedDebugAndroidTest` (needs a device/emulator). They are the only thing that
exercises migrations against the real `AndroidSqliteDriver` — **run them before shipping any schema
change.** Gotcha: `kotlin.assert()` is a no-op on ART; always use `kotlin.test.assertTrue`.

Two migrations are **destructive**, and they are the reason that suite exists. `3.sqm` rebuilds
`transactions` because SQLite cannot change a column's type; `4.sqm` rebuilds `transactions` AND
`recurring_movements` because it cannot add a table constraint either, and repairs the data first —
with foreign keys ON the `INSERT INTO transactions_new SELECT` is validated against the new key as it
copies, so repairing afterwards would repair rows that never crossed.
`MigrationV3ToV4Test` and `MigrationV4ToV5Test` are what say the rows, the indexes, the types and the
ability to open the app at all survive them. Two of `MigrationV4ToV5Test`'s cases migrate with
**foreign keys ON**, the only configuration under which statement order in `4.sqm` matters at all —
a device never provides it, so a suite that only reproduced the device path would pass whatever order
the migration were written in. Why that is worth testing anyway: `work/epics/E02-migration-coverage.md`.

The coverage invariant — one test per *starting* version, not per migration step — is
`work/epics/E02-migration-coverage.md`.

## Migration tests: use raw SQL against historical schemas

SQLDelight generates query classes from the CURRENT schema. A migration test that builds a historical
schema (e.g. v2 via a custom `SqlSchema`) and then calls generated queries
(`accountsQueries.insert(...)`) FAILS with `table accounts has no column named syncState` — the
generated INSERT references columns the historical schema doesn't have yet.

Pattern (see `MigrationV1ToV2Test`, `MigrationV2ToV3Test`):

- Set-up inserts against the historical schema: `driver.execute(null, "INSERT INTO ... raw SQL ...", 0)`.
- Reads within the historical window: `driver.executeQuery(null, sql, { cursor -> QueryResult.Value(...) }, 0).value`
  — the mapper must return `QueryResult<T>`, not `T`.
- **Migrate to `EmmDatabaseData.Schema.version`, not to the next one.** Generated queries only match
  the CURRENT schema, so a test that stops mid-chain cannot use them for its assertions — and that is
  also what a real device does, since it runs the whole chain in one open. `MigrationV2ToV3Test`
  stopped at 3 and started failing the moment a 4 existed.
- After `Schema.migrate(...)` reaches the current version, generated queries work for assertions.
