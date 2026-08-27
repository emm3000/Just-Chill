# E07-06 — Name `:androidApp`'s detekt resolution errors

**Epic:** [E07 — baseline burndown](../epics/E07-baseline-burndown.md)

## Done when

- [ ] the symbols `:androidApp:detektDevDebug` fails to resolve are named, and the source files
      referencing them
- [ ] every detekt rule that silently stopped firing on `:androidApp` because of them is listed —
      that list, not the error count, is the point of this ticket
- [ ] the remaining errors are placed against `BuildConfig` — E01-37's fix (the module's own Kotlin
      classes on detekt's `classpath`) already took `devDebug` from 15 to 13 by resolving the
      generated `BuildInfo.kt`, and `BuildConfig` is Java, so no Kotlin output dir will carry it
- [ ] either the errors are gone, or the closing commit records why detekt `2.0.0-alpha.6` cannot fix
      it and `docs/CODE_QUALITY.md` names the limitation beside the `:data` one
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`:androidApp:detektDevDebug` prints `There were N compiler errors found during analysis. This affects
accuracy of reporting.` and still exits 0. The count is non-deterministic — 12 to 25 across runs of
the same sources — which is itself worth explaining.

Confirmed pre-existing during E09-02, by running the task in a worktree at `4ea73a6a`, before that
ticket touched any code. E01-37 settled the shared half — `excludeGeneratedSources` did drop
generated code from the resolution scope — and what survives here is the Java-generated remainder.
