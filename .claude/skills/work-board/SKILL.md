---
name: work-board
description: "Trigger: ticket, epic, backlog, tablero, plan a unit, close a unit, where does this fact go. Operate the docs/work file-per-ticket board."
license: Apache-2.0
allowed-tools: Read, Grep, Glob, Bash, Write, Edit
metadata:
  author: "emm"
  version: "2.0"
---

## Activation Contract

Load when planning a unit of work, closing one, recording a defect or invariant found mid-work, or deciding where a fact belongs. Load also when a live doc is about to gain prose describing work.

## Hard Rules

- Read `docs/work/README.md` first. It is the only source of the board's rules; nothing below repeats one. If a rule appears in both places, delete it here.
- Verify a constraint against the source before writing it into an epic. A plan in this repo asserted `BackupOrchestrator` holds the shared `SyncMutex`; the file contains no lock at all. Prose is never verified, so a false claim in it survives indefinitely.
- Tick a `Done when` box only after the check that proves it has run. Name that check when reporting.

## Decision Gates

**If it has a fix, it is a ticket, not a constraint.** A constraint describes how the system is and can be broken by accident; a defect describes how the system is wrong and gets closed.

## Execution Steps

1. On taking a ticket, re-verify its `Done when` against current code. Correct the ticket first if the code moved under it.
2. Close it in the same commit as the code it describes.
3. Defect found mid-work: open a new ticket. Never widen the ticket in `doing/`.
4. After moving or archiving any doc: `git grep -l '<old-path>' -- ':!docs/archive'` and repoint every hit.

## Output Contract

Report the ticket IDs touched and their new directory, plus any ticket created, plus the check that proved each box ticked.

## References

- `docs/work/README.md` — the rules.
- `docs/work/epics/E01-snapshot-backup.md` — worked example of an epic.
- `assets/ticket.md`, `assets/epic.md` — templates.
