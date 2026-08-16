---
name: work-board
description: "Trigger: ticket, epic, backlog, tablero, plan a unit, close a unit, where does this fact go. Operate the docs/work file-per-ticket board."
license: Apache-2.0
allowed-tools: Read, Grep, Glob, Bash, Write, Edit
metadata:
  author: "emm"
  version: "1.0"
---

## Activation Contract

Load when planning a unit of work, closing one, recording a defect or invariant found mid-work, or deciding where a fact belongs. Load also when a live doc is about to gain prose describing work.

## Hard Rules

- The board's rules live in `docs/work/README.md`. Read it. Never restate them here or in another doc — a rule in two places diverges.
- Never write a list of ticket names into any file. The index is `eza docs/work/backlog`.
- Never write chronicle into a ticket or an epic: why a past round decided something, how finished work operates. That belongs in the commit message and engram.
- Verify a constraint against the source before writing it. A plan in this repo asserted `BackupOrchestrator` holds the shared `SyncMutex`; the file contains no lock at all. Prose is never verified, so a false claim in it survives indefinitely.
- Every `Done when` entry must be observably true or false. "Improve the flow" is not one.

## Decision Gates

| The fact | Goes to |
|---|---|
| Work with a fix and an end state | new ticket in `backlog/` |
| Describes how the system IS; a writer could break it by accident | epic `## Constraints` |
| Why a decision was made, or how closed work operates | commit message + engram |
| A rule for operating the board itself | `docs/work/README.md` |

Test: **if it has a fix, it is a ticket, not a constraint.**

## Execution Steps

1. Take: `git mv docs/work/backlog/<id>-*.md docs/work/doing/`.
2. Tick a `Done when` box only after the check that proves it has run.
3. Close: `git mv docs/work/doing/<id>-*.md docs/work/done/`, in the same commit as the code it describes.
4. New ticket: copy `assets/ticket.md`, take the next free `E<NN>-<NN>`. Never reuse or renumber a retired ID.
5. Defect found mid-work: open a new ticket. Do not widen the ticket in `doing/`.
6. After moving or archiving any doc: `git grep -l '<old-path>' -- ':!docs/archive'` and repoint every hit.

## Output Contract

Report the ticket IDs touched and their new directory, plus any ticket created. Never report a `Done when` as met without naming the check that proved it.

## References

- `docs/work/README.md` — the rules.
- `docs/work/epics/E01-snapshot-backup.md` — worked example of an epic.
- `assets/ticket.md`, `assets/epic.md` — templates.
