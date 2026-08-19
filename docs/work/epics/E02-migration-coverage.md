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
  chain with them ON — the iOS shape, stricter than the Android upgrade path. `MigrationV1ToV2Test`
  does; dropping that callback would keep it green while proving less.
- The per-test mechanics (raw SQL against historical schemas, `QueryResult<T>` mappers,
  `kotlin.assert()` being a no-op on ART) live in `data/CLAUDE.md` under
  `### Migration tests`. They are not repeated here.
