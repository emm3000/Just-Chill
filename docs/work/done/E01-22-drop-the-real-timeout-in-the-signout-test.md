# E01-22 — Drop the real Ktor timeout in the sign-out test

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] `signOut clears the local session and reports LocalOnly when the logout request hangs` no
  longer depends on a real-time deadline elapsing, and still fails if the local clear stops running
- [x] `SHORT_REQUEST_TIMEOUT` is gone, or its remaining uses cannot decide a test's outcome by how
  loaded the machine is
- [x] the suite passes under `qualityGate --rerun-tasks` on a loaded machine, not only when run alone

## Context

`SHORT_REQUEST_TIMEOUT` is `500.milliseconds` of **real** Ktor request timeout raced against a
`MockEngine` that never answers, inside a `runTest` whose scheduler is virtual — so wall clock, not
the test, decides. Observed failing 1 run in 4 during E01-21's review. E01-20's sweep cannot see
this one: it greps `System.nanoTime|Thread.sleep`, and this deadline is neither.
