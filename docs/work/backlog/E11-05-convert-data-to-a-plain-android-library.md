# E11-05 — Convert :data to a plain Android library

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-04

## Done when

- [ ] `data/build.gradle.kts` applies `com.android.library` and `org.jetbrains.kotlin.android`, not
      `justchill.kmp.library`.
- [ ] Every `.sq` and `.sqm` moved from `commonMain/sqldelight` to `src/main/sqldelight` under the
      same file name, and the SQLDelight database configuration points at it.
- [ ] `src/main/kotlin` holds what `commonMain` and `androidMain` held; `src/test/kotlin` holds what
      `commonTest` and `androidHostTest` held; `src/androidTest/kotlin` holds what `androidDeviceTest`
      held.
- [ ] `src/main/AndroidManifest.xml` is still the module's manifest.
- [ ] `./gradlew :data:assembleDebugAndroidTest` succeeds and the six migration tests plus
      `RecurringMovementFkTest` and `DeleteUseCasesE2ETest` still compile.
- [ ] `./gradlew qualityGate --rerun-tasks` runs `:data:testDebugUnitTest`.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.

## Context

The device suite is E02's migration coverage — read `docs/work/epics/E02-migration-coverage.md` and
`docs/PERSISTENCE.md` before moving a `.sqm`.
