---
description: Self-review pending changes against the repo rules
allowed-tools: Bash(git status:*) Bash(git diff:*) Read Grep
disable-model-invocation: true
---

Review the pending changes (staged + unstaged) against `CLAUDE.md` and `.claude/rules/`. Use `git status`, `git diff` and `git diff --cached` to see what changed.

## Checklist

1. **Layer boundaries**
   - Any file under `core/domain/` importing Android, SQLDelight, Supabase or Ktor types?
   - Any file under `presentation/src/main` importing Compose, `BuildConfig`, `R.`, `koin.androidx` or MockK?
   - Any production file under `ui-android/` or `androidApp/` importing a `:core:domain` repository or a `Default*` implementation?
   - Allowed dependencies: `androidApp -> ui-android, presentation, data, domain`; `ui-android -> presentation, data, domain`; `presentation -> data, domain`; `data -> domain`.

2. **MVI**
   - New features have `UiState`, `Intent`, `Effect`, and `onIntent(intent)` on an `MviViewModel`?
   - Naming: `*ViewModel`, `*Screen`, `*Entries.kt`, `*UiState`, `*Intent`, `*Effect`, `*Route`?
   - State stores ids, never resolved objects; no literal Spanish string in a ViewModel?
   - A new ViewModel bound once in `hh/di/` and added to `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`?

3. **UI**
   - Any raw Material3 control (`Button`, `TextField`, `OutlinedTextField`, `Card`, `IconButton`, a `Switch` without `emmSwitchColors`)?
   - Any literal `Color(0x...)`, `.sp` or `.dp` outside `:core:ui`'s `core/ui/theme/`?
   - New shared components live in `core/ui/atoms/` with a `@Preview` in `EmmTheme`?
   - Every new route `@Serializable`?

4. **Detekt (config/detekt/detekt.yml)**
   - Nesting within detekt `NestedBlockDepth` (allowedDepth 4, a fifth level fails)? No nested `also/apply/run/let`? ≤ 2 returns per function (labeled returns excluded)?
   - A new baseline entry only for a pre-existing finding, with its GitHub issue?

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
