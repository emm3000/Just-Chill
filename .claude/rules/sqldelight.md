---
paths:
  - "data/src/main/sqldelight/**"
  - "data/src/androidTest/**/Migration*Test.kt"
---

# Schema and migrations

Read `docs/PERSISTENCE.md` before changing anything here.

- A `.sq` CREATE TABLE never changes without a matching `.sqm`; never reset the schema. The author's device holds the oldest real data, and a missing migration only fires there.
- A schema bump regenerates the snapshot (`:data:generateDebugEmmDatabaseDataSchema`); `:data:verifySqlDelightMigration` runs on `qualityGate`.
- Coverage is one instrumented test per starting version, each migrating to `EmmDatabaseData.Schema.version`, never to the next step.
- Migration tests use raw SQL against historical schemas; generated queries work only after the chain reaches the current version. `kotlin.assert()` is a no-op on ART, use `kotlin.test.assertTrue`.
- Files move freely but never rename: a `.sqm` digit is the version it migrates from, a `.sq` name is its generated `<Name>Queries` class.
