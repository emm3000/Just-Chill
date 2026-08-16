# Per-target gates

Read the row for the target before opening any source file.

| Target | Read first | File budget |
|---|---|---|
| Feature / screen flow | `docs/DESIGN_SYSTEM.md`, then the screen, its ViewModel, its use cases | 15 |
| Architecture / module | `CLAUDE.md` `## Architecture`, then that module's own `CLAUDE.md` (only `androidApp/`, `ui-android/`, `presentation/`, `domain/`, `data/` ship one; `build-logic/`, `iosApp/`, `supabase/` have none) | 20 |
| Sync | `docs/work/epics/E01-snapshot-backup.md` and `docs/adr/009-backup-is-a-snapshot-not-row-replication.md` BEFORE any source file. The forensic audit is archived at `docs/archive/sync/AUDIT.md` — read it for *why*, never for what to do next | 20 |
| Supabase / SQLDelight migrations | `docs/archive/sync/AUDIT.md`, then `supabase/migrations/` against the SQLDelight schema. Flag any drift — this is the class that broke production for two months (commit `72a9b03`) | 15 |
| `:presentation` commonMain | the exported-iOS surface. Any `java.*` or `android.*` reference is CRITICAL; the only proof is `./gradlew :presentation:compileKotlinIosSimulatorArm64`, which the MAIN THREAD runs — the auditor reports the suspect imports it found and marks the finding UNPROVEN | 20 |
| Dates | `docs/DATE_AUDIT.md`; live rule #7 is an injected `Clock` + `TimeZone`, neither carrying a default | 15 |
| `.github/` pipelines | `CLAUDE.md` `## Gotchas` — pinned SHAs, the `git describe --match "v[0-9]*"` filter, secrets via `env:` | 10 |
| Docs vs code | the code is the truth, the doc is the suspect | 20 |
| Installed skills / config | judge which earn their place against the repo's stack. Do not rewrite them; that is `skill-improver` | 10 |
| Whole project | do not attempt exhaustively. Sample the rows above, declare the sample, and emit `## Cobertura` | 30 |

## Scope budget

- The budget counts source files opened, not the docs in the `Read first` column.
- On hitting the budget, stop and emit `## Cobertura`. Never silently truncate.
- The auditor never runs Gradle — no `qualityGate`, no compile task. When a finding can only be proven by a Gradle task, report it as UNPROVEN and name the exact command for the main thread to run.
