# KMP Migration — Orchestration Workflow

> How we execute each slice of the KMP / Compose Multiplatform migration.
> Carved out after Slices 1–3 proved the loop. Companion to
> `MIGRATION_PLAN.md` and `PHASE_3_SPEC.md`.

## Roles

- **Orchestrator** (main thread, Opus): scopes the slice, delegates, verifies
  cheaply, keeps its own context clean. Does NOT write feature code inline.
- **Writer** (delegated sub-agent, **Opus 4.8**): does the actual move +
  de-JVM + Koin split for one slice. Fresh context, full task prompt.
- **Reviewer** (delegated sub-agent, **Opus 4.8**, fresh context): adversarial
  double-check of the writer's commit. Different agent instance — no shared
  context with the writer. Finds breakage / risky changes the gate can't catch.

Both writer and reviewer are Opus 4.8. The user does not trust mechanical KMP
work to Sonnet; this migration runs Opus end to end.

## Per-slice loop

```
1. Orchestrator SCOPES (inline, cheap)
2. Orchestrator delegates → WRITER (Opus)        ── writes + commits the slice
3. Orchestrator CHEAP-VERIFY (inline)            ── git state + leak grep
4. Orchestrator delegates → REVIEWER (Opus)      ── adversarial, fresh context
5. SHIP (reviewer verdict) or FIX-FIRST loop back to writer
```

### 1. Scope (orchestrator, inline)
Before delegating, map the slice cheaply so the writer prompt is precise:
- List the feature's files (`fd -e kt`), check for test files.
- `rg` for leaks: `import (java|javax|android)\.`, `BuildConfig`, `R.`/
  `stringResource`/`painterResource`/`Font(R.`, `@Preview`/`tooling.preview`,
  `LocalConfiguration`, `koin.androidx`.
- Read the feature's Koin module — does it bind a `:data` repository
  (`DefaultXRepository`)? That binding must split to `:app`'s `HhModule`.
- This stays inline (1–3 reads + greps). Do NOT read the whole feature.

### 2. Writer prompt must include
- Repo root, branch (`kmp/phase-0-scaffolding`), "commit here, do NOT branch/push".
- Pointers to read: `PHASE_3_SPEC.md` STATUS banner + the slice, the relevant
  engram memos (`kmp/phase-3/slice-N-*`), the previous slice's commit hash.
- Exact file list to move, package-paths-unchanged rule.
- Pre-scoped blockers from step 1 (Preview strip, koin swap, Koin split, de-JVM).
- Dependency-closure directive: map transitive `:app` imports FIRST; co-move
  non-platform deps (watch `internal→public` flips); hoist platform symbols to
  callbacks wired by the nav host in `:app`.
- The reinforced gate (below).
- Conventions: English code, Spanish only for UI strings; `rg`/`fd`/`bat`/`sd`;
  conventional commit, **NO Co-Authored-By**; linear history; no push.
- Save gotchas to engram (`kmp/phase-3/slice-N-*`, cross-ref prior slices).

### 3. Cheap-verify (orchestrator, inline)
- `git log --oneline` — commit landed.
- `rg -l 'import (java|javax|android)\.' shared-ui/src/commonMain` — must be empty.
- Confirm the moved dir is gone from `app/src/main`.
- Do NOT re-run the full build here (expensive) — the reviewer does that.

### 4. Reviewer prompt must include
- "FRESH context, adversarial, do NOT trust the writer's self-report."
- The commit hash + what the slice was supposed to do.
- Checklist: diff scope sane; no duplicates/leftovers; **Koin graph bound
  exactly once** (zero = runtime crash the Android gate misses, double = also
  wrong); java/android leak grep; behavior preservation (moved bodies
  byte-identical except documented swaps); cross-module same-package gotcha;
  **re-run the full gate from scratch** (`--rerun-tasks`).
- Output: severity-tagged findings (`CRITICAL`/`WARNING`/`NIT`) + one-line
  verdict **SHIP** or **FIX-FIRST**. Review only — no fixes, no commit.

### 5. Decide
- **SHIP** → report to user, move to next slice.
- **FIX-FIRST** → send findings back to the writer agent (or a fix agent) and
  re-review.

## The reinforced gate (every slice)
```bash
./gradlew :shared-ui:compileAndroidMain
./gradlew :shared-ui:compileKotlinIosSimulatorArm64   # proves zero java.* leak
./gradlew assembleDevDebug
./gradlew :app:testDevDebugUnitTest
./gradlew :shared-ui:testAndroidHostTest
```
The iOS compile is the real proof the de-JVM worked. The Android gate alone does
NOT catch a missing Koin binding — that's why the reviewer re-runs + traces DI.

## Why two Opus agents per slice
The Android build can be green while DI is broken (missing repository binding →
runtime crash only when the screen opens). A fresh-context reviewer that traces
the Koin graph and re-runs the gate catches what the writer (and the compiler)
miss. Cost is justified: a broken slice merged silently costs more than one
review pass. Confirmed worth it on Slice 3 (DI was the single highest-risk spot).

## Established patterns (carry across slices)
- **commonMain depends on `:domain` only, NOT `:data`** → feature Koin modules
  in commonMain wire domain use cases only; `:data` repo bindings live in
  `:app`'s `HhModule`.
- **De-JVM playbook** (app is Spanish-only → no `expect/actual`, no locale
  machinery): `java.time`→`kotlinx-datetime`; localized Spanish dates→hardcoded
  tables (`hh/shared/SpanishDateFormat`); `DecimalFormat`→`NumberFormatEs`;
  `Normalizer`→`SpanishSearch`; `java.util.UUID`→`kotlin.uuid.Uuid`;
  `koin.androidx.compose.koinViewModel`→`koin-compose-viewmodel`.
- **CMP swaps**: `@Preview` takes no params (strip); import
  `androidx.compose.ui.tooling.preview.Preview` — the CMP 1.10+ multiplatform
  annotation, provided by the `org.jetbrains.compose.ui:ui-tooling-preview`
  dependency (NOT `compose.components.uiToolingPreview`, which shipped the
  now-deprecated `org.jetbrains.compose.ui.tooling.preview.Preview` namespace);
  `LocalConfiguration`→`LocalWindowInfo`+`LocalDensity`.
- **Package paths never change** when files cross the module boundary — keeps
  `:app` imports from churning. `:app` consumers of moved same-package symbols
  need explicit imports added (implicit same-package resolution stops at the
  module boundary).
- **Tests**: MockK is JVM-only → MockK VM tests stay in `:app`. Pure
  `kotlin-test`/JUnit tests can move to `shared-ui/commonTest`.

## Slice ledger
| Slice | Feature | Commit | Status |
|---|---|---|---|
| 0 | scaffold + MVI + theme | `7d66e54` | ✅ |
| 1 | transactions + de-JVM | `4b10bad` | ✅ |
| 2 | categories | `1f35f32` | ✅ |
| 3 | accounts | `5701f95` | ✅ (writer+reviewer) |
| 4 | recurring | `591cbcc` | ✅ (writer+reviewer) |
| 5 | home + report | `b82f128` | ✅ (writer+reviewer) |
| 6 | onboarding | `66df00e` | ✅ (inline — trivial, 1 file) |
| 7a | auth | `eed7778` | ✅ (writer+reviewer — reviewer caught a RED gate the writer falsely reported green; fixed before ship) |
| 7b | profile | `ecdd111` | ✅ (writer+reviewer) |
| 8a | cleanup — agnostic atoms + orphan deletion + fonts drop | `9d7cfa4` | ✅ (writer+reviewer) |
| 8b | cleanup — hhModule DI sweep | `8e50b30` | ✅ (writer+reviewer — Phase 3 closed) |

## Phase 7 — dedup ledger (one Compose base)

> Distinct from the Phase 3 feature-migration above. Goal: kill Android/iOS
> duplication, push platform-specifics behind `expect/actual`. Slices A–F +
> cleanup, all writer+reviewer Opus except the low-risk cleanup (writer +
> cheap-verify). Branch `kmp/phase-0-scaffolding`, linear, not pushed.

| Slice | What | Commit | Status |
|---|---|---|---|
| A | platform-neutral core/components shell → commonMain | `c849091` | ✅ |
| B | `expect/actual resumeEvents()` + establish shared-ui/androidMain | `d251760` | ✅ |
| C | AppPreferences + cursor store → commonMain over multiplatform-settings (keystone) | `402c26d` | ✅ |
| D | merge 2 SyncOrchestrators → 1 commonMain class + `SyncController.events` | `352a97c` | ✅ |
| E1 | nav route keys + BottomBar + ProfileMessage → commonMain; nav3-runtime → common | `a02c09b` | ✅ |
| E2 | iOS sync retry snackbar + Manifesto first-launch gate (commonMain handlers) | `de26928` | ✅ |
| cleanup | hoist replaceAll, drop orphan dep + unused atom, fix stale docs | `0784014` | ✅ (writer + cheap-verify) |
| F | merge Android + iOS nav hosts → one commonMain `AppNavHost`; Android → JetBrains nav3-UI | `186d3b6` | ✅ (writer+reviewer Opus; net −254) |

### Carry-forward decisions / landmines
- **Nav host UNIFIED (Option A reversed, slice F `186d3b6`).** Both platforms now
  run ONE commonMain `AppNavHost` on the JetBrains nav3-UI port (Google nav3-UI
  dropped from `:androidApp`). The earlier HELD decision (predictive-back regression
  risk) was lifted by a spike: `enableOnBackInvokedCallback` is absent from every
  manifest, so the app never opted into the predictive-back gesture — there was
  nothing to regress. Device process-death restore verified on both the spike and
  slice F. The 5 platform-divergent capabilities (export/import/share/email/privacy)
  live behind `expect/actual PlatformHostActions`.
- **Nav-state landmine (now BOTH platforms):** since slice F, Android also uses the
  2-arg `rememberNavBackStack(navSavedStateConfiguration, …)`. Every route the host
  can push MUST be registered in `NavSavedStateConfiguration.kt` (commonMain), else
  `rememberNavBackStack` crashes on process-death restore. K/N has no reflection
  serializer discovery; Android's explicit config must stay complete too. Invisible
  to the compiler + the Android compile gate — only process-death restore catches it.
- **Build.ID prefs bug FIXED** (`4d2e204`): the SharedPreferences file was named
  after `Build.ID` → wiped on every OS update. Now stable `justchill_prefs` +
  one-time migration. (Not a dedup slice; Android-only `CoreModule`.)
