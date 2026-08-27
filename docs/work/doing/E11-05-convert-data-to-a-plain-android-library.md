# E11-05 — Convert :data to a plain Android library

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-04

## Done when

- [x] `data/build.gradle.kts` applies `com.android.library` plus `justchill.detekt` and
      `justchill.quality.gate` directly, and no `org.jetbrains.kotlin.android`.
- [x] Every `.sq`, `.sqm` and `databases/*.db` snapshot moved from `commonMain/sqldelight` to
      `src/main/sqldelight` under its own name, and `schemaOutputDirectory` points at the new path.
- [x] `src/main/kotlin` ← `commonMain` + `androidMain`; `src/test/kotlin` ← `commonTest` +
      `androidHostTest`; `src/androidTest/kotlin` ← `androidDeviceTest`.
- [x] `withDeviceTest { instrumentationRunner }` became `defaultConfig { testInstrumentationRunner }`,
      `add("commonMainApi", ...)` became a plain `api(platform(libs.supabase.bom))`, and both
      `kotlin("test")` became `kotlin("test-junit")`.
- [x] **`QualityGateConventionPlugin.COMPILE_GATE_TASKS` names the new instrumented compile task.**
      It holds `compileAndroidDeviceTest` today, and its KDoc says why detekt cannot replace it.
      `DETEKT_GATE_TASKS` needs the same for `detektAndroidDeviceTestSourceSet`.
- [x] `verifySqlDelightMigration` still runs — it aggregates `verifyCommonMainEmmDatabaseDataMigration`,
      whose name embeds the source set.
- [x] `./gradlew :data:assembleDebugAndroidTest` succeeds and the six migration tests,
      `RecurringMovementFkTest` and `DeleteUseCasesE2ETest` all compile.
- [x] The gate runs `:data:testDebugUnitTest`, the executed count matches the `@Test` count, and no
      `baseline-data*.xml` is orphaned or gains an entry.
- [x] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass.

## Context

Read `docs/PERSISTENCE.md` and `docs/work/epics/E02-migration-coverage.md` first.
