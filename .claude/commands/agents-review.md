---
description: Self-review pending changes against the repo rules
allowed-tools: Bash(git status:*) Bash(git diff:*) Read Grep
disable-model-invocation: true
---

Review the pending changes (staged + unstaged) against `CLAUDE.md` and `.claude/rules/`. Use `git status`, `git diff` and `git diff --cached` to see what changed.

## Checklist

1. **Layer boundaries**
   - Any file under `core/domain/` importing Android, SQLDelight, Supabase or Ktor types?
   - Any `*ViewModel.kt` or `*UiState.kt` importing Compose, `BuildConfig`, `R.`, `koin.androidx` or MockK?
   - Any production file outside `:androidApp`'s `core/di/` and `wiring/` naming a `Default*` repository or data source?
   - Allowed dependencies: `androidApp -> feature:*, core:backup, core:database, core:ui, core:domain`; `feature:* -> core:ui, core:domain` (plus `core:testing` on the test edge); every `core:*` -> `core:domain`.

2. **MVI**
   - New features have `UiState`, `Intent`, `Effect`, and `onIntent(intent)` on an `MviViewModel`?
   - Naming: `*ViewModel`, `*Screen`, `*Entries.kt`, `*UiState`, `*Intent`, `*Effect`, `*Route`?
   - State stores ids, never resolved objects; no literal Spanish string in a ViewModel?
   - A new ViewModel bound once in its feature's `<feature>Module` and added to `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`?

3. **UI**
   - Any raw Material3 control (`Button`, `TextField`, `OutlinedTextField`, `Card`, `IconButton`, a `Switch` without `emmSwitchColors`)?
   - Any literal `Color(0x...)`, `.sp` or `.dp` outside `:core:ui`'s `core/ui/theme/`?
   - New shared components live in `core/ui/atoms/` with a `@Preview` in `EmmTheme`?
   - Every new route `@Serializable`?

4. **Complexity (`.claude/rules/kotlin-style.md`, review-enforced)**
   - More than 4 levels of nesting? No nested `also/apply/run/let`? ≤ 2 real returns per function?
   - A function or file doing several things that should be split?

5. **Local-first and data**
   - Any code assuming a backend, row sync or a mandatory login?
   - A `CREATE TABLE` change carries its `.sqm`, its `databases/N.db` and a migration test per starting version?
   - `Clock` and `TimeZone` injected, no defaults? Every catch-all rethrows `CancellationException` first?

6. **Hygiene**
   - Comments beyond the three exceptions in `kotlin-style.md`? Explicit types on properties and locals?
   - Spanish only in user-facing values, tuteo never voseo; English identifiers?
   - Sensitive files in the diff (`keystore.properties`, `local.properties`, `key/`, `google-services.json`)?

## Output

For each violation: `file:line` + rule + suggestion on a single line. If everything is clean, reply **"clean — ready to commit"**. Do not edit files in this turn.
