# E11-07 — Retire the KMP convention plugin and purge the catalog

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-06

## Done when

- [ ] `KmpLibraryConventionPlugin.kt` is deleted, `build-logic/build.gradle.kts` registers no
      `justchill.kmp.library` and declares neither KMP plugin marker, and the root `build.gradle.kts`
      declares neither `apply false`.
- [ ] `Project.contributeToQualityGate` goes with it — `KmpLibraryConventionPlugin` was its only caller.
- [ ] `gradle/libs.versions.toml` declares no `skie`, `kotlin-multiplatform`,
      `android-kotlin-multiplatform-library`, `jetbrains-lifecycle-viewmodel` or `jetbrainsLifecycle`
      entry. All five are already unreferenced outside the catalog.
- [ ] `rg 'jetbrains\.kotlin\.multiplatform|android\.kotlin\.multiplatform\.library|touchlab\.skie'
      --glob '*.gradle.kts' --glob '*.toml'` matches nothing.
- [ ] No `*.gradle.kts` names `justchill.kmp.library`. Five carry a comment about it — `:data`,
      `:presentation`, `:ui-android`, `:androidApp`, `:domain` — and a comment about a symbol that
      no longer exists is chronicle, which lives in git.
- [ ] `:domain` still builds, and the `kotlin-jvm` alias's comment states the true reason for
      whatever version state it ends in: it is unversioned today only because build-logic's
      `kotlin-multiplatform` marker supplies the jar, and this ticket deletes that marker.
- [ ] `./gradlew :build-logic:test` passes and `QualityGateConventionPlugin` still names it explicitly.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` pass.

## Context

Every module is off KMP as of E11-06, so nothing applies the plugin or resolves these entries.
