# E11-07 — Retire the KMP convention plugin and purge the catalog

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-06

## Done when

- [ ] `KmpLibraryConventionPlugin.kt` is deleted and `build-logic/build.gradle.kts` registers no
      `justchill.kmp.library`.
- [ ] `gradle/libs.versions.toml` declares no `skie`, `kotlin-multiplatform`,
      `android-kotlin-multiplatform-library`, `jetbrains-lifecycle-viewmodel` or `jetbrainsLifecycle`
      entry.
- [ ] `rg -i 'multiplatform' --glob '*.gradle.kts' --glob '*.toml'` matches nothing but
      `multiplatform-settings`.
- [ ] `kotlin-jvm` in the catalog carries an explicit version. It is unversioned today only because
      build-logic's `kotlin-multiplatform` marker puts the kotlin-gradle-plugin jar on the buildscript
      classpath; deleting that marker without pinning here stops `:domain` building.
- [ ] `./gradlew :build-logic:test` passes and `QualityGateConventionPlugin` still names it
      explicitly.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.
