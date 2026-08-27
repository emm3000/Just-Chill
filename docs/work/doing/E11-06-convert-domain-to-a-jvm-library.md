# E11-06 — Convert :domain to a JVM library

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-05

## Done when

- [ ] `domain/build.gradle.kts` applies `org.jetbrains.kotlin.jvm` and no Android or multiplatform
      plugin; the catalog gains the `kotlin-jvm` alias it needs.
- [ ] `src/main/kotlin` holds what `commonMain` held and `src/test/kotlin` what `androidHostTest`
      held, moved by `git mv` alone — that commit shows `0 insertions(+), 0 deletions(-)`.
- [ ] `rg 'android\.|androidx\.' -g '*.kt' domain/src` returns nothing.
- [ ] `DETEKT_GATE_TASKS` no longer names `detektMainAndroid`, `detektCommonTestSourceSet` or
      `detektAndroidHostTestSourceSet`, and its KDoc drops the last-KMP-module claim.
- [ ] `qualityGate --dry-run`, diffed against the same output captured before the change, loses no
      coverage: `:domain:test`, `:domain:detektMain` and `:domain:detektTest` replace the four
      `:domain` tasks the gate runs today.
- [ ] `:domain:test` executes 333 tests (`domain/build/test-results/test/*.xml`) — today's `@Test` count.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` pass.

## Context

Last module on purpose — a KMP `commonMain` cannot resolve a JVM-only artifact.
`justchill.kmp.library` is the only caller of `contributeToQualityGate("testAndroidHostTest")`, so
dropping it strands `:domain`'s tests off the gate unless the build file names `test`, as `:data` does.
