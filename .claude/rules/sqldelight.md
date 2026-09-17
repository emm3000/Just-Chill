---
paths:
  - "data/src/main/sqldelight/**"
  - "data/src/androidTest/**/Migration*Test.kt"
---

# SQLDelight schema rules

`EmmDatabaseData` on device is the source of truth. Every schema change is a migration unit with its own falsifier. Why `3.sqm` and `4.sqm` are destructive: `data/CLAUDE.md`. The category/type composite key: ADR 008 and the header of `transactions.sq`.

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
- Foreign keys: `csm()`'s `onOpen` turns them on only after the upgrade chain ran (they cannot be switched on inside `SQLiteOpenHelper`'s upgrade transaction), and SQLite never re-checks rows already written, so an FK-violating row written by a migration is silent on device forever. A test that enables foreign keys in its own `onOpen` and then calls `Schema.migrate` is the only check of the chain's writes; `MigrationV1ToV2Test` does it and `MigrationV4ToV5Test` flips them on for the cases where `4.sqm`'s statement order matters. Never drop that callback: the test stays green while proving less.
- Run the suite with `./gradlew :data:connectedDebugAndroidTest` on the `medium_phone` emulator before shipping any schema change; the gate only compiles it.

## The restore drill

The suite proves a migration keeps rows already on the device, never that a snapshot written before the bump still restores after it. Before shipping a bump, run it on `medium_phone` with the dev flavor (`com.emm.justchill.dev`), never on a second AVD and never with a seeded database file:

1. Install the pre-bump build (`./gradlew installDevDebug` from `trunk`), import the latest production snapshot through the app, and record the six `ImportStats` counts (`accounts`, `categories`, `transactions`, `recurring`, `loans`, `loanPayments`).
2. The owner wipes the dev app's data. Agents are denied `adb uninstall` and `adb shell pm clear`, so this step is always the owner's.
3. Install the post-bump build, import the same file, and compare the six counts. Any count that moved is a failure.

Restore is the only way back from a migration that loses data, so a bump never restored from is untested.
