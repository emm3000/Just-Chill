# E11-04 — Convert :presentation to a plain Android library

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-03

## Done when

- [ ] `presentation/build.gradle.kts` applies `com.android.library` plus `justchill.detekt` and
      `justchill.quality.gate` directly, and **no** `org.jetbrains.kotlin.android` — AGP 9's
      built-in Kotlin support rejects that plugin outright.
- [ ] `libs.jetbrains.lifecycle.viewmodel` is replaced by a new `androidx.lifecycle:lifecycle-viewmodel`
      catalog entry, and the module still declares no Compose dependency.
- [ ] `implementation(kotlin("test"))` is `kotlin("test-junit")` — plain AGP drops the wiring that
      made `kotlin.test.Test` resolve.
- [ ] `src/main/kotlin` holds what `commonMain` and `androidMain` held; `src/test/kotlin` holds what
      `commonTest` and `androidHostTest` held.
- [ ] `AppGraphKoinTest` and `TestPlatformModule` still run in the gate — the missing-Koin-binding
      net survives — and the executed test count matches the repo's `@Test` count.
- [ ] `qualityGate --dry-run` before and after accounts for every `:presentation` task that leaves:
      `detektMainAndroid`, `detektCommonTestSourceSet`, `detektAndroidHostTestSourceSet` and
      `testAndroidHostTest` each have a named, type-resolved replacement already in
      `QualityGateConventionPlugin.DETEKT_GATE_TASKS`.
- [ ] `baseline-presentation-main.xml` is renamed to what the new tasks look for, with no orphan and
      no entry added.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass.

## Context

`commonTest` and `androidHostTest` share no filename, so the merge into `src/test` cannot collide.
`androidMain` holds only `CommitHash.kt`.
