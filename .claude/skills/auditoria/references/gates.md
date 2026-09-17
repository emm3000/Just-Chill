# Per-target gates

Read the row for the target before opening any source file.

| Target | Read first | File budget |
|---|---|---|
| Feature / screen flow | `.claude/rules/ui-components.md`, then the screen, its ViewModel, its use cases | 15 |
| Architecture / module | `CLAUDE.md` `## Architecture`, then that module's own `CLAUDE.md` (only `androidApp/`, `ui-android/`, `presentation/`, `core/domain/`, `data/` ship one; `build-logic/` and `supabase/` have none) and the `.claude/rules/` file scoped to it | 20 |
| Sync | `data/CLAUDE.md` `## Backup`, `presentation/CLAUDE.md` `## Backup` and `docs/adr/009-backup-is-a-snapshot-not-row-replication.md` BEFORE any source file | 20 |
| Supabase / SQLDelight migrations | `.claude/rules/sqldelight.md` and `data/CLAUDE.md` `## Persistence`, then `supabase/migrations/` against the SQLDelight schema. Flag any drift — this is the class that broke production for two months (commit `72a9b03`) | 15 |
| Dates | `.claude/rules/architecture.md` `## Layers and dependency direction` — the live rule is an injected `Clock` AND an injected `TimeZone`, neither carrying a default, and it names its own allowed exceptions | 15 |
| `.github/` pipelines | `.claude/rules/github-workflows.md` — pinned SHAs, the `git describe --match "v[0-9]*"` filter, secrets via `env:` | 10 |
| Docs vs code | the code is the truth, the doc is the suspect | 20 |
| Whole project | do not attempt exhaustively. Sample the rows above, declare the sample, and emit `## Coverage` | 30 |

## Scope budget

- The budget counts source files opened, not the docs in the `Read first` column.
- On hitting the budget, stop and emit `## Coverage`. Never silently truncate.
