# E01-02 — Verify backup in-app

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] "Verify backup" downloads the newest snapshot pair, checks the payload bytes against the
  manifest's `payloadSha256`, and parses the payload without applying it
- [x] the check walks back to the newest snapshot that verifies, rather than reporting the newest
  pair as broken when only its manifest is bad or of unknown state
- [x] the UI names which snapshot verified, and the per-table counts it reports come from the file —
  never a comparison against live local data, which legitimately moves between backups
- [x] `:presentation` reaches all of it through a domain port; `BackupObjectStore` and
  `decodePayload` stay `internal` to `:data`

## Context

`ProfileOp.VerifyingBackup` does not exist, and two guards emit `OperationInProgress` — verify must
pick one: `launchOp` (`ProfileViewModel.kt:158`) is generic, `backUpNow` (`:138`) bypasses it and
re-implements the ladder inline. Nothing decodes a `.manifest.json` back into `BackupManifestDto`.
