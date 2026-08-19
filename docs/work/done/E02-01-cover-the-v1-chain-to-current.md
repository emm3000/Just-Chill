# E02-01 — Cover the v1 chain to the current schema

**Epic:** [E02 — Migration coverage](../epics/E02-migration-coverage.md)

## Done when

- [x] A v1 database seeded with rows in every table it has reaches `EmmDatabaseData.Schema.version`
      in one `Schema.migrate` call, and its rows are asserted after it.
- [x] `rg 'newVersion = \d' data/src/androidDeviceTest/` returns no hits.
- [x] `./gradlew :data:connectedAndroidDeviceTest` passes on a device or emulator.

## Context

`MigrationV1ToV2Test:117` migrates to a literal `2`; the other three migration tests all target
`EmmDatabaseData.Schema.version`, so `1 → current` is the one starting version nothing covers.
Its set-up inserts stay raw SQL against the v1 schema; the post-migration assertions become
generated queries — the pattern is already written in `data/CLAUDE.md` `### Migration tests`.
