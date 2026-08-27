# E11-02 — Collapse the platform-split files

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-01

## Done when

- [ ] `rg '^\s*(expect|actual)\s' -g '*.kt'` returns nothing across the repo — five `expect`
      declarations exist today: `ioDispatcher`, `isSqliteConstraintViolation`, `isSqliteException`,
      `backgroundEvents`, `resumeEvents`.
- [ ] `Dispatchers.kt` and `SqliteExceptions.kt` (`:data`), `BackgroundEvents.kt` and
      `ResumeEvents.kt` (`:presentation`) each exist once, under `commonMain`, with the Android body
      inlined.
- [ ] `fd -e kt --glob '*.android.kt'` returns nothing. `DatabaseDriver.android.kt` carries no
      `expect`/`actual`, but its suffix means nothing with one platform left.
- [ ] `'android'` is gone from `MatchingDeclarationName.multiplatformTargets` in
      `config/detekt/detekt.yml` and detekt is still green — that entry exists only to exempt
      `*.android.kt` filenames.
- [ ] `provideSqlDriver` still resolves from `:androidApp`'s `AndroidPlatformModule`, and the
      default-category seed SQL it installs is byte-identical to before.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.

## Context

`presentation/src/androidMain/.../CommitHash.kt` stays where it is — it has no `commonMain`
counterpart, and emptying `androidMain` is E11-04's flattening, not this ticket's job.
