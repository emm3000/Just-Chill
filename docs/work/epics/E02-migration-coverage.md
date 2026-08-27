# E02 — Migration coverage

## Why

The instrumented migration suite is the only thing that runs a schema change against a real
`AndroidSqliteDriver`, and the author's own device holds the oldest data in existence. A migration
that loses rows is the one failure this repo cannot undo.

## Constraints

- Coverage is per **starting version**, not per migration step. A device opens once and runs
  `1 → current` in a single `Schema.migrate` call, so every historical version needs its own test
  reaching `EmmDatabaseData.Schema.version`. A test that proves `N → N+1` proves nothing about the
  chain a device actually executes.
- A migration test that stops mid-chain still passes as long as its reads are raw SQL. Passing is
  therefore not evidence the chain works — only the target version of `Schema.migrate` is.
- A test that enables foreign keys in `onOpen` and then calls `Schema.migrate` itself runs the whole
  chain with them ON, which no device ever does — `SQLiteOpenHelper` owns the upgrade transaction
  and foreign keys cannot be switched on inside one, so `csm()`'s `onOpen` turns them on only after
  the chain has run, and SQLite never re-checks rows already written. A migration that writes an
  FK-violating row is therefore silent on device and stays silent; this is the only check of the
  chain's own writes against the schema it declares. `MigrationV1ToV2Test` does it; dropping that
  callback would keep it green while proving less.
- The per-test mechanics (raw SQL against historical schemas, `QueryResult<T>` mappers,
  `kotlin.assert()` being a no-op on ART) live in `docs/PERSISTENCE.md` under
  `## Migration tests`. They are not repeated here.
