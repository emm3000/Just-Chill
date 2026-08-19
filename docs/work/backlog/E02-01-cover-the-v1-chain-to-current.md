# E02-01 — Cover the v1 chain to the current schema

**Epic:** [E02 — Migration coverage](../epics/E02-migration-coverage.md)

## Done when

- [ ] A v1 database seeded with rows in every table it has reaches `EmmDatabaseData.Schema.version`
      in one `Schema.migrate` call, and its rows are asserted after it.
- [ ] `rg 'newVersion = \d' data/src/androidDeviceTest/` returns no hits.
- [ ] `./gradlew :data:connectedAndroidDeviceTest` passes on a device or emulator.

## Context

`MigrationV1ToV2Test:117` migrates to a literal `2`; the other three migration tests all target
`EmmDatabaseData.Schema.version`, so `1 → current` is the one starting version nothing covers.
It passes today only because its reads are raw SQL — decide whether they stay raw or become
generated queries once the test reaches the current schema, before writing the fix.
