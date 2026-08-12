# KMP Migration — Starting Point (Baseline)

> **Archived.** The migration this is a baseline for is merged, so nothing measures itself
> against these numbers any more. Kept for the reasoning and for the rollback hash. The live
> KMP doc is `docs/kmp/ORCHESTRATION.md`; current state is `docs/PROGRESS.md`. Module names
> below are pre-migration (`:app` is now `:androidApp`, and the UI lives in `:ui-android`).

> The known-good Android reference captured **before** any KMP change.
> Every later phase claims "Android still green" relative to THIS.

**Captured**: 2026-06-12
**Branch work starts on**: `kmp/phase-0-scaffolding` (off `trunk`)

## Git
- trunk HEAD at start: `947db68` — `docs(kmp): add Android+iOS KMP migration plan`
- Rollback: `git reset --hard 947db68` (no tag; hash + reflog cover it).

## Modules (pre-migration)
```
:app      android.application   (Jetpack Compose, 196 kt, 15 screens, 14 VMs, 9 Koin modules)
:data     android.library       (SQLDelight + Supabase + Ktor/OkHttp + sync)
:domain   java-library + kotlin.jvm   (pure Kotlin, zero java.*/android.*)
```

## Actual versions (verified from libs.versions.toml — NOT the stale CLAUDE.md)
| Lib | Version | Note |
|-----|---------|------|
| Kotlin | **2.4.0** | ⚠️ CLAUDE.md says 2.3.21 — STALE. Compose Multiplatform must match 2.4.0 |
| Koin BOM | 4.2.1 | KMP-ready |
| Supabase BOM | 3.6.0 | KMP-ready |
| Ktor | 3.5.0 | OkHttp engine (Android); iOS needs Darwin engine same version |
| SQLDelight | 2.3.2 | KMP-ready |
| AGP | 9.2.1 | KMP lib plugin available |

## Green reference (the gate definition)
```bash
./gradlew assembleDevDebug      # MUST pass — this is "Android is green"
./gradlew :domain:test          # domain unit tests
```
**Baseline build status**: confirmed green at HEAD 947db68 before Phase 0 edits.

## Implications for the plan
- Plan §0 said "Kotlin 2.3.21" — corrected to **2.4.0**. Pick the Compose
  Multiplatform release built against Kotlin 2.4.0.
- Ktor 3.5.0 is the pin: `ktor-client-darwin` must be 3.5.0 to match OkHttp.
