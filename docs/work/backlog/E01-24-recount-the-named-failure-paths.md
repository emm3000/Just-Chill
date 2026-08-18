# E01-24 — Recount the epic's named failure paths

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] the epic's failure-path count matches what `data/backup/` actually spells out
- [ ] the line says how to recount it, so the next writer does not have to rediscover the method

## Context

`docs/work/epics/E01-snapshot-backup.md:18` says "fourteen named failures today". The literal
enumeration is about 18 — uploader 10, pruner 4, verifier 4 — and was already stale before E01-11.
The number is a running count nobody can verify without recounting, which is why it drifted.
