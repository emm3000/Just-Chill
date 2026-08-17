# E01-03 — Documented restore drill

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] `data/CLAUDE.md` states that a schema bump also restores the latest production snapshot on a
  clean emulator and compares it against the pre-bump state, beside the existing
  `:data:connectedAndroidDeviceTest` rule that fires on the same trigger
- [x] the drill names what "compare" means concretely enough to fail — which tables, which counts —
  rather than leaving it to whoever reads it under pressure

## Context

There is no release checklist: `.claude/commands/release.md` is the only release surface and it
fires per release, not per schema bump — the wrong trigger, and weeks after the migration was
written. `data/CLAUDE.md:106` carries the sibling rule and sits at 134 of its 150-line ceiling.
