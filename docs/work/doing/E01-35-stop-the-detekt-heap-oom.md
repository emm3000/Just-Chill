# E01-35 — Stop the detekt heap OOM

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `./gradlew qualityGate --rerun-tasks` either survives ten consecutive runs, or the report is
      recorded as unreproducible and this ticket dies
- [ ] if it reproduces, the fix raises the heap the detekt worker actually gets — never a retry, a
      `--no-daemon` workaround, or a baseline entry hiding the file
- [ ] the closing commit states the run count and the setting that settled it

## Context

Seen under detekt 2.0.0-alpha.6, always green on the identical re-run: twice on
`:androidApp:detekt*DebugUnitTest` analysing `BackupOrchestratorTest.kt`, then — on a clean tree at
`3370b6c8` — in two of three consecutive `qualityGate --rerun-tasks`, one killing
`:ui-android:detektDebug` on `CategoryResolve.kt` and the next taking that task and both
`:androidApp` siblings at once. A third module, task and file end the "one pathological file"
reading, and the frequency moved from twice in weeks to twice in three runs.

There is no detekt worker to hand heap to: `detekt.use.worker.api` defaults to false, so analysis
runs in the Gradle daemon and `org.gradle.jvmargs` is the only lever. `c77746d9` (E01-37) is the
trigger, not the cause. A gate that is red once and green on re-run is a gate people learn to
re-run.
