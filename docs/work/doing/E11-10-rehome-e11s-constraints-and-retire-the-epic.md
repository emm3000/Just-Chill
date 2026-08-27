# E11-10 — Rehome E11's constraints and retire the epic

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-09

## Done when

- [ ] Two of the epic's constraints are deleted as duplicates, not moved: `docs/CODE_QUALITY.md`
      already owns the per-variant baseline split under `### One baseline file per analysis task`,
      and `presentation/CLAUDE.md` already owns the no-Compose rule with its check command.
- [ ] The `kotlin-jvm` pin invariant lives in root `CLAUDE.md` `## Gotchas` — nothing states it
      today, and dropping the pin downgrades `:domain`'s stdlib silently. `CLAUDE.md` stays ≤ 150.
- [ ] `:domain`'s reason for being `kotlin("jvm")` — the only mechanical thing left stopping
      `android.*` from reaching the core — survives where a writer reads it before touching `:domain`.
      Root `CLAUDE.md` states the layout today but not why.
- [ ] `multiplatform-settings` staying survives, and its catalog comment stops describing `commonMain`
      and `NSUserDefaultsSettings (iOS)`. E11-08's grep covered `docs/` and `*CLAUDE.md` only, so it
      never reached `gradle/libs.versions.toml`.
- [ ] The `.sq`/`.sqm` change-path-never-name rule lives with E02's coverage invariant; neither
      `E02-migration-coverage.md` nor `PERSISTENCE.md` states it today.
- [ ] `docs/work/epics/E11-android-only.md` is in `docs/archive/`, and `git grep -l 'E11-android-only'
      -- ':!docs/archive'` finds nothing.
- [ ] The diff touches only `*.md` and comment lines in `gradle/libs.versions.toml` — no code,
      no build logic, so no gate task can regress.

## Context

Every E11 ticket is closed, so the epic is a plan with nothing remaining. Its invariants are not: six
of them outlive the track, and three have no other home.
