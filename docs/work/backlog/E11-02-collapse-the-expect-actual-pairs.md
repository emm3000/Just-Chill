# E11-02 — Collapse the six expect/actual pairs

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-01

## Done when

- [ ] `rg '^\s*(expect|actual)\s' -g '*.kt'` returns nothing across the repo.
- [ ] `Dispatchers.kt` and `SqliteExceptions.kt` (`:data`), `BackgroundEvents.kt` and
      `ResumeEvents.kt` (`:presentation`) each exist once, under `commonMain`, with the Android body
      inlined.
- [ ] No `*.android.kt` file remains in `:data` or `:presentation`.
- [ ] No `androidMain` source set holds a `.kt` file.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.

## Context

Blocks every flattening ticket — `commonMain` and `androidMain` merge into one source set, and an
`expect` cannot sit beside its `actual` there.
