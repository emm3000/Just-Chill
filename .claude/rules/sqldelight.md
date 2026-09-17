---
paths:
  - "data/src/main/sqldelight/**"
  - "data/src/androidTest/**/Migration*Test.kt"
---

# SQLDelight schema rules

`EmmDatabaseData` on device is the source of truth. Every schema change is a migration unit with its own falsifier. The full reasoning, the destructive migrations and the restore drill: `docs/PERSISTENCE.md`, read before changing anything here.

## The snapshot is part of the diff

- A change to any `.sq` that alters a `CREATE` statement ships three artifacts in the same commit: the `.sq` edit, `com/emm/data/N.sqm` where `N` is the version before the bump, and `databases/(N+1).db`.
- Generate the snapshot with `./gradlew :data:generateDebugEmmDatabaseDataSchema`. It writes the current version to `databases/`.
- `./gradlew :data:verifySqlDelightMigration` replays every `.sqm` over the snapshots and runs on `qualityGate`. It cannot notice a snapshot that was never written, so the `.db` is checked by the reviewer.
- Never delete or regenerate a committed `.db`, and never reset the schema. Each one is the exact schema a shipped build wrote to disk, and the author's device holds the oldest real data: a missing migration only fires there.
- Files move freely but never rename: a `.sqm` digit is the version it migrates from (mirrored by its test's `oldVersion`), a `.sq` name is its generated `<Name>Queries` class.

## Writing the migration

- Additive only: `ALTER TABLE ... ADD COLUMN` appended last, nullable or with a default. Renames, drops and new constraints go through a new table plus a copy, with the data repaired before the copy when foreign keys could reject rows (`4.sqm` is the pattern).
- **The new column goes at the END of `CREATE TABLE` too.** `ADD COLUMN` appends, so a column placed mid-table leaves a fresh install and a migrated install disagreeing on column order; `verifySqlDelightMigration` reports it as an `ordinalPosition - CHANGED` mismatch.
- Deletes are soft (`deletedAt`, `syncState = 'Pending'`) and every read query filters `deletedAt IS NULL`. Foreign-key clauses fire only on physical `DELETE`; deletion integrity lives above the schema.
- `transactions.occurredAt` is ISO local text with no zone; `createdAt` / `updatedAt` / `deletedAt` are epoch millis. Keep that split.

## The migration test

- Coverage is **one instrumented test per starting version** under `data/src/androidTest/`, each migrating to `EmmDatabaseData.Schema.version`, never to the next step: a device opens once and runs the whole chain in one `Schema.migrate` call.
- Set-up and in-chain reads use raw SQL against the historical schema (`driver.execute`, `driver.executeQuery` returning `QueryResult`); generated queries match only the current schema and work for assertions once the chain reaches it. `MigrationV1ToV2Test` is the pattern.
- Never use `Schema.create` in a migration test: it builds the latest schema and skips the migration under test.
- `kotlin.assert()` is a no-op on ART; use `kotlin.test.assertTrue`.
- Prove the test is not vacuous before trusting it: set its `oldVersion` to the current schema version so the migration is skipped, watch it fail on the missing column, restore it.
- Run the suite with `./gradlew :data:connectedDebugAndroidTest` on the `medium_phone` emulator before shipping any schema change; the gate only compiles it.
