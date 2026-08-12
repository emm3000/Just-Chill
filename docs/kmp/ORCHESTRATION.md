# KMP Migration — Orchestration Workflow

> How we execute each slice of the KMP / Compose Multiplatform migration.
> Carved out after Slices 1–3 proved the loop. Its two companions were archived once
> the migration merged — `../archive/kmp/MIGRATION_PLAN.md` and
> `../archive/kmp/PHASE_3_SPEC.md`, both historical and both carrying decisions that
> were later reversed. This file is the one that stays current.

## Roles

- **Orchestrator** (main thread, Opus): scopes the slice, delegates, verifies
  cheaply, keeps its own context clean. Does NOT write feature code inline.
- **Writer** (delegated sub-agent, **Opus**): does the actual move + de-JVM +
  Koin split for one slice. Fresh context, full task prompt.
- **Reviewer** (delegated sub-agent, **Opus**, fresh context): adversarial
  double-check of the writer's commit. Different agent instance — no shared
  context with the writer. Finds breakage / risky changes the gate can't catch.

Both writer and reviewer are Opus — the current top model, whatever its version
number is at the time. The user does not trust mechanical KMP work to Sonnet;
this work runs Opus end to end.

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
- Pointers to read: `../archive/kmp/PHASE_3_SPEC.md` STATUS banner + the slice, the relevant
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
- `rg -l 'import (java|javax|android)\.' ui-android/src/commonMain` — must be empty.
- Confirm the moved dir is gone from `androidApp/src/main`.
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
./gradlew qualityGate      # the whole gate
./gradlew assembleDevDebug # plus the build, which the gate deliberately excludes
```

**Do not maintain a task list here.** This section used to spell one out, and it drifted: it was
missing `:data:detektAndroidDeviceTestSourceSet`, while the pre-push hook was missing every test
and CI was missing `:ui-android:testAndroidHostTest`. Four hand-written lists, none a superset of
the others. The gate now has exactly one definition —
`build-logic/src/main/kotlin/com/emm/buildlogic/QualityGateConventionPlugin.kt` — and the hook, the
three workflows and this doc all invoke it.

`qualityGate` covers, per module: detekt over every source set that holds code, the JVM host test
suites, Android lint on the dev variant, and the iOS compile. To change what the gate means, edit
the plugin; everything downstream follows.

Two properties worth knowing:

- **The iOS compile is host-gated.** Kotlin/Native only builds iOS binaries on macOS, so the gate
  adds it there and logs the skip elsewhere — CI on Linux runs everything else. It is the real proof
  the de-JVM worked, and per `docs/adr/003` it is the one thing keeping frozen iOS revivable.
- **iosMain detekt has no type resolution** (Kotlin/Native has none in detekt 2.0), so TR-dependent
  rules do not fire there. Treat it as style and structure, not a deep semantic gate. It is JVM
  analysis, though, so it runs on any host — including CI.

A green gate still does NOT catch a missing Koin binding at the DI graph level; that is what
`ui-android/androidHostTest/core/AppGraphKoinTest.kt` is for, and it is now on the gate (it never
ran in CI before).

Never gate on plain `./gradlew detekt`: it is **NO-SOURCE on every KMP module** and only ever
linted `:androidApp`.

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
  `kotlin-test`/JUnit tests can move to `ui-android/commonTest`.

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
> duplication, push platform-specifics behind `expect/actual`. Slices A–F (dedup)
> + cleanup + G (iOS-capability fill-in), all writer+reviewer Opus except the
> low-risk cleanup (writer + cheap-verify). Branch `kmp/phase-0-scaffolding`,
> linear, not pushed.

| Slice | What | Commit | Status |
|---|---|---|---|
| A | platform-neutral core/components shell → commonMain | `c849091` | ✅ |
| B | `expect/actual resumeEvents()` + establish ui-android/androidMain | `d251760` | ✅ |
| C | AppPreferences + cursor store → commonMain over multiplatform-settings (keystone) | `402c26d` | ✅ |
| D | merge 2 SyncOrchestrators → 1 commonMain class + `SyncController.events` | `352a97c` | ✅ |
| E1 | nav route keys + BottomBar + ProfileMessage → commonMain; nav3-runtime → common | `a02c09b` | ✅ |
| E2 | iOS sync retry snackbar + Manifesto first-launch gate (commonMain handlers) | `de26928` | ✅ |
| cleanup | hoist replaceAll, drop orphan dep + unused atom, fix stale docs | `0784014` | ✅ (writer + cheap-verify) |
| F | merge Android + iOS nav hosts → one commonMain `AppNavHost`; Android → JetBrains nav3-UI | `186d3b6` | ✅ (writer+reviewer Opus; net −254) |
| G | implement iOS `PlatformHostActions` (export/import/share/email via UIKit) + enable privacy policy | `139518a` | ✅ (writer+reviewer Opus; modals need sim verify) |
| H | dedup Koin platform-wiring → one commonMain copy each + injected `platformModule`; reverse commonMain `:domain`-only rule (commonMain → `:data`) | `56314ba` | ✅ (writer+reviewer Opus; net −210; iOS Koin runtime needs sim verify) |

### Carry-forward decisions / landmines
- **Nav host UNIFIED (Option A reversed, slice F `186d3b6`).** Both platforms now
  run ONE commonMain `AppNavHost` on the JetBrains nav3-UI port (Google nav3-UI
  dropped from `:androidApp`). The earlier HELD decision (predictive-back regression
  risk) was lifted by a spike: `enableOnBackInvokedCallback` is absent from every
  manifest, so the app never opted into the predictive-back gesture — there was
  nothing to regress. Device process-death restore verified on both the spike and
  slice F. The 5 platform-divergent capabilities (export/import/share/email/privacy)
  live behind `expect/actual PlatformHostActions`.
- **Nav-state landmine — REVERSED, the registry is gone.** Slice F had put Android on
  the 2-arg `rememberNavBackStack(navSavedStateConfiguration, …)` and required every
  route to be registered in `NavSavedStateConfiguration.kt`, because the ONE shared
  host also ran on K/N, which has no reflection serializer discovery. ADR 005 sent
  iOS to native SwiftUI and `:ui-android` went Android-only, so that host — and the
  registry's only reason to exist — is gone. `AppNavHost` now takes the Android-only
  1-arg `rememberNavBackStack(startRoute)` and resolves entries by JVM reflection.
  What survives is the weaker obligation: every route MUST be `@Serializable`, still
  invisible to the compiler + the compile gate, still only caught by process-death
  restore — and now by `RouteSerializationTest`, which round-trips every sealed
  `AppRoute` through the very serializer the host runs.
- **Build.ID prefs bug FIXED** (`4d2e204`): the SharedPreferences file was named
  after `Build.ID` → wiped on every OS update. Now stable `justchill_prefs` +
  one-time migration. (Not a dedup slice; Android-only `CoreModule`.)
- **iOS UIKit delegate-retention landmine (slice G):** a Kotlin/Native `NSObject`
  delegate (e.g. `UIDocumentPickerDelegateProtocol`) is held by UIKit via a WEAK
  ref → it gets GC'd before the user finishes → the modal silently fires nothing.
  Fix used: a module-level `mutableSetOf<NSObject>()` that retains the delegate
  from creation until it removes itself inside EVERY terminal callback. Invisible
  to the compile gate. The export/import/share/email/mail modals are compile- +
  launch-verified only — their live round-trips MUST be driven on the iOS simulator
  by a human (no idb/XCUITest here).
- **commonMain → :data layering REVERSED (slice H `56314ba`).** Was `:domain`-only (a pre-KMP rail). ui-android/commonMain now depends on `:data`, so the DI wiring (`syncModule`/`authModule`/`dataModule`/`supabaseModule`) is ONE commonMain copy parameterized by a per-platform `platformModule` (DB single + seed, `Settings` backend, `SupabaseConfig`, `GoogleSignInLauncher`, `appVersion`, `googleServerClientId`). Tradeoff: lost the compile-time guardrail that blocked a ViewModel importing `Default*Repository`/SQLDelight types — VM purity is now CONVENTION only. `startKoin{}` itself can't be shared (Android needs koin-android `androidContext`/`androidLogger`, absent in commonMain); only the module list (`appModules`) + post-start `bootstrapAppGraph` (claim observer + `orchestrator.start()`) are shared. Koin failures are RUNTIME-ONLY (invisible to the compiler AND the Android gate) — iOS `initKoin()` runtime resolution STILL needs a human simulator run; static bind-trace + `compileKotlinIosSimulatorArm64` are green but that is not a device launch.
- **detekt 2.0 per-task baseline scheme (how to keep the gate green).** detekt 2.0
  derives a SEPARATE baseline file per analysis task from the extension stem
  `config/detekt/baseline-<module>.xml` (set once in the root `build.gradle.kts`
  `subprojects {}` block — no per-task config needed). Mapping:
  `detektMainAndroid` ↔ `baseline-<module>-main.xml`; `detektIosMainSourceSet` ↔
  `baseline-<module>-iosMainSourceSet.xml`; `:androidApp:detektMain` fans out to
  `baseline-androidApp-{devDebug,devRelease,prodDebug,prodRelease}.xml`. The old
  stem files (`baseline-<module>.xml`) belong to the plain `detekt` task (NO-SOURCE
  on KMP) + the pre-push hook — leave them ALONE. Because each task derives its own
  path, two baseline tasks for the same module never overwrite each other (the
  feared 2.0 overwrite landmine does not apply here). To grandfather pre-existing
  issues for a source set, run the matching baseline task and COMMIT the generated
  file, e.g. `./gradlew :ui-android:detektBaselineMainAndroid
  :ui-android:detektBaselineIosMainSourceSet` → commit `baseline-ui-android-main.xml`
  + `baseline-ui-android-iosMainSourceSet.xml`. NEVER baseline to dodge a NEW
  violation a slice introduces — fix it; baselines only grandfather what predates
  the detekt gate (initial counts: ui-android main 154 / iosMain 10, data main 589 /
  iosMain 2, domain main 1, androidApp devDebug 136 / prodDebug 8).
