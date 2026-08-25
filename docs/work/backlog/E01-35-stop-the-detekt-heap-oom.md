# E01-35 — Stop the detekt heap OOM

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `./gradlew qualityGate --rerun-tasks` either survives ten consecutive runs, or the report is
      recorded as unreproducible and this ticket dies
- [ ] if it reproduces, the fix raises the heap the detekt worker actually gets — never a retry, a
      `--no-daemon` workaround, or a baseline entry hiding the file
- [ ] the closing commit states the run count and the setting that settled it

## Context

Seen twice under detekt 2.0.0-alpha.6, each time green on the identical re-run: during the E05-08
review `:androidApp:detektProdDebugUnitTest` died with `java.lang.OutOfMemoryError: Java heap space`
analysing `BackupOrchestratorTest.kt`, and during E07-05 the dev-flavor sibling
`:androidApp:detektDevDebugUnitTest` died the same way. Two flavors, so it is heap pressure on the
worker, not one file. A gate that is red once and green on re-run is a gate people learn to re-run.
