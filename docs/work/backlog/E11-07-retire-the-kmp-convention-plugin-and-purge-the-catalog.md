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
- [ ] `./gradlew :build-logic:test` passes and `QualityGateConventionPlugin` still names it
      explicitly.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.
