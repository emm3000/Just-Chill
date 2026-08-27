# E11-03 — Convert :ui-android to a plain Android library

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-02

## Done when

- [ ] `ui-android/build.gradle.kts` applies `com.android.library` and `org.jetbrains.kotlin.android`,
      plus `justchill.detekt` and `justchill.quality.gate` directly — `justchill.kmp.library` used to
      supply those two. `androidApp/build.gradle.kts` is the working template.
- [ ] `src/main/kotlin` holds what `androidMain/kotlin` held, `src/main/res` what `androidMain/res`
      held, and `src/test/kotlin` what `androidHostTest` held.
- [ ] `androidResources { enable = true }` is gone and the bundled Inter / IBM Plex Mono families and
      the Google sign-in vector still render **on a device or emulator** — the failure mode is a
      runtime `MissingResourceException`, invisible to the build.
- [ ] The `ui-tooling` dependency is on `debugImplementation`, not `androidRuntimeClasspath`.
- [ ] `composeCompiler.stabilityConfigurationFiles` still resolves `compose_stability.conf`.
- [ ] `qualityGate --dry-run` before and after shows every `:ui-android` source set still linted:
      `detektMainAndroid` and `detektAndroidHostTestSourceSet` are replaced by named tasks that are
      in `QualityGateConventionPlugin.DETEKT_GATE_TASKS`, not dropped.
- [ ] `:ui-android`'s detekt baseline still resolves under its new task names, and
      `config/detekt/` holds no orphaned `baseline-ui-android-*.xml`.
- [ ] `./gradlew qualityGate --rerun-tasks` runs `:ui-android:testDebugUnitTest`.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass.
