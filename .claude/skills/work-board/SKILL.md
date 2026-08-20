---
name: work-board
description: "Trigger: ticket, epic, backlog, tablero, plan a unit, close a unit, where does this fact go. Operate the docs/work file-per-ticket board."
license: Apache-2.0
allowed-tools: Read, Grep, Glob, Bash, Write, Edit
metadata:
  author: "emm"
  version: "3.0"
---

## Activation Contract

Load when planning a unit of work, closing one, recording a defect or invariant found mid-work, or deciding where a fact belongs. Load also when a live doc is about to gain prose describing work.

## Hard Rules

- Read `docs/work/README.md` first. It is the only source of the board's rules; nothing below repeats one. If a rule appears in both places, delete it here.
- **Audit a ticket before working it, not after.** Its text is a claim written before the work existed. Three checks, all before the first edit — a ticket that fails any of them gets corrected first, in its own commit:
  1. Does each `Done when` still match the code? Symbols move.
  2. Can each one be checked by something you can actually run? Delete or replace a condition nothing can falsify.
  3. Does its premise hold? "No coverage", "nothing pins this", "X calls Y" are claims and each needs its own grep. Verifying half a premise and reporting the whole is the failure mode.
- A ticket that states a fix direction has already made a design decision. Confirm the direction before implementing it, and weigh what each option costs when it is wrong.
- Verify a constraint against the source before writing it into an epic. Prose is never verified, so a false claim in it survives indefinitely.
- Tick a `Done when` box only after the check that proves it has run. Name that check when reporting.

## Decision Gates

**If it has a fix, it is a ticket, not a constraint.** A constraint describes how the system is and can be broken by accident; a defect describes how the system is wrong and gets closed.

## Execution Steps

1. Take: `git mv` into `doing/`, after the audit above.
2. Defect found mid-work: open a new ticket. Never widen the ticket in `doing/`.
3. Before closing, ask whether the decision the ticket settled can be undone by accident. If it can, it earns one constraint line in the epic, in the same commit.
4. Close on proof that actually ran. A gate reporting mostly `UP-TO-DATE` re-verified nothing; force it.
5. Close it in the same commit as the code it describes.
6. After moving or archiving any doc: `git grep -l '<old-path>' -- ':!docs/archive'` and repoint every hit.

## Output Contract

Report the ticket IDs touched and their new directory, any ticket created, the check that proved each box ticked, and any correction made to the ticket before working it.

## References

- `docs/work/README.md` — the rules.
- `docs/work/epics/E01-snapshot-backup.md` — worked example of an epic.
- `assets/ticket.md`, `assets/epic.md` — templates.
