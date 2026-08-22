# E05-11 — Rehearse the v6 restore

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocks:** shipping any build carrying schema v6

## Done when

- [ ] the latest production snapshot is imported onto a clean emulator running the pre-bump build,
      and its six `ImportStats` counts are recorded
- [ ] the same file is imported on the v6 build and the six counts are unchanged
- [ ] a v4 file exported from the v6 build restores onto a second clean emulator with every count
      intact

## Context

`docs/PERSISTENCE.md` demands this per schema bump: the migration suite proves rows already on the
device survive, never that a snapshot written *before* the bump can still be read *after* it.
Only the author can do it — it needs the real production snapshot, which no test fixture
substitutes for. The mechanism is covered by host tests (E05-05); this is the one step they cannot
stand in for.
