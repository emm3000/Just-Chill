# E05-16 — Archive the E05 epic

**Epic:** [E05 — Loans](../epics/E05-loans.md)

## Done when

- [ ] each of the six constraints in `E05-loans.md` is either rehomed (ADR 010, `PERSISTENCE.md`
      or `E01-snapshot-backup.md`) or deleted as a duplicate of a line already there — one verdict
      per constraint, each verified against the source before it moves
- [ ] `E05-loans.md` lives in `docs/archive/` and
      `git grep -l 'epics/E05-loans' -- ':!docs/archive'` returns nothing (ADR 010 links it today)

## Context

E05-11 was the last ticket under E05. An epic with nothing remaining is chronicle; its constraints
are the only part a future writer can break by accident, so each needs a live home before the file
moves. E11-10 (`git log --grep 'close E11-10'`) is the precedent.
