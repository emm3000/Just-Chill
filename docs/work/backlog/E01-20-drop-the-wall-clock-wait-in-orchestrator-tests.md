# E01-20 — Drop the wall-clock wait in the orchestrator health tests

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `awaitUntil` and its `AWAIT_TIMEOUT_MILLIS` / `NANOS_PER_MILLI` constants are gone from
      `BackupOrchestratorHealthTest`
- [ ] each of its four call sites waits on the emission it needs instead of polling, and still fails
      when the emission never arrives
- [ ] `rg 'System.nanoTime|Thread.sleep' --glob '*Test.kt'` returns nothing across the repo

## Context

`awaitUntil` polls with `Thread.sleep(1)` against a 5s wall-clock deadline, so a loaded machine
fails the `check` at line 375 rather than the assertion. It failed once inside a full `qualityGate`
and passed three times out of three alone. This is the only test file in the repo using wall-clock.
