# docs/work — file-per-ticket

Directory is the status. `backlog/`, `doing/`. Closing a ticket is `git rm` — there is no status
field inside a ticket file, and no `done/`: a closed ticket is chronicle, and the chronicle lives in
git. A status that lives in two places diverges; a ticket that outlives its work is an essay.

IDs are immutable: `E<epic>-<ticket>`, e.g. `E01-04`. Never renumbered, never reused — the number
dies with the ticket, even if the ticket is abandoned.

Ceilings: ticket ≤ 30 lines, epic ≤ 80 lines, this README ≤ 40 lines. A file over its ceiling gets
cut, not extended.

Division of labour:
- **ticket** = the work — a `Done when` list of falsifiable conditions, plus at most 3 lines of
  context a writer needs to start.
- **epic** = the constraints — invariants that outlive every ticket under it, the kind a future
  writer could break by accident.
- **git + engram** = the chronicle — why a decision was made, what a past round tried. Never
  written into a ticket or an epic.

There is no hand-maintained index anywhere — not in an epic, not in this README, not in
`PROGRESS.md`. The index is `eza docs/work/backlog` (or `doing/`, `done/`). If you are about to
write a list of ticket names into another file, stop: that list is the exact thing this structure
exists to kill.

Naming: `E<NN>-<NN>-<kebab-slug>.md` — epic number, ticket number, short slug. Epics live in
`docs/work/epics/E<NN>-<kebab-slug>.md`.

**Citations are by symbol, not by line.** A line number in prose is a guarantee nothing enforces.
Name the symbol and let `rg` find it.
