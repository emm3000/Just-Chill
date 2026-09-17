# Domain Docs

How the engineering skills consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`docs/adr/`**: the ADRs that touch the area you are about to work in. This is a single-context
  repo, so every ADR lives there, numbered `NNN-<decision>.md`
  (`009-backup-is-a-snapshot-not-row-replication.md`); the filename states the decision and the
  header declares what it amends or supersedes.
- **`CONTEXT.md`** at the repo root: the glossary, grouped by Ledger, Recurring, Loans, Report,
  Backup and Identity, each term with the words to avoid. A glossary and nothing else; a term is
  added or changed through `mattpocock-skills:domain-modeling`, never by hand in passing.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a
test name), use the term as `CONTEXT.md` defines it. Don't drift to synonyms the glossary avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language
the project doesn't use (reconsider) or there's a real gap (note it for `domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR 007 (writer and reviewer are separate agents), but worth reopening because…_
