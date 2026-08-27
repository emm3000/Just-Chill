# E11-06 — Convert :domain to a JVM library

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-05

## Done when

- [ ] `domain/build.gradle.kts` applies `org.jetbrains.kotlin.jvm` and no Android or multiplatform
      plugin.
- [ ] `src/main/kotlin` holds what `commonMain` held and `src/test/kotlin` holds what
      `androidHostTest` held.
- [ ] `rg 'android\.|androidx\.' -g '*.kt' domain/src` returns nothing.
- [ ] `./gradlew qualityGate --rerun-tasks` runs `:domain:test`, and detekt still covers the module.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.

## Context

Last module on purpose — a KMP `commonMain` cannot resolve a JVM-only artifact, so `:data` and
`:presentation` must already be plain Android libraries.
