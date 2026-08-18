# E01-24 — Recount the epic's named failure paths

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] the epic's failure-path count matches what `data/backup/` actually spells out — the epic now
      carries no count to drift; it names the command that recounts instead
- [x] the line says how to recount it, so the next writer does not have to rediscover the method

## Context

`docs/work/epics/E01-snapshot-backup.md:18` says "fourteen named failures today". An independent
recount over `data/src/commonMain/kotlin/com/emm/data/backup/` (one distinct reason per
`storageCall`/`wholeBucket`/explicit-throw site inside `DefaultBackupUploader`,
`DefaultBackupPruner`, `DefaultBackupVerifier`) gives **uploader 8, pruner 4, verifier 4 = 16**, not
18 — and materially more, up to roughly 22, once `BackupManifestDto.kt`'s six payload/manifest
reasons and `SupabaseBackupObjectStore.kt`'s own throws are folded in. The count depends on where
the producer boundary is drawn; it was already stale before E01-11 either way.
