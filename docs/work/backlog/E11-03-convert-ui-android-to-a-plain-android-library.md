# E11-03 — Convert :ui-android to a plain Android library

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-02

## Done when

- [ ] `ui-android/build.gradle.kts` applies `com.android.library` and
      `org.jetbrains.kotlin.android`, not `justchill.kmp.library`.
- [ ] `src/main/kotlin` holds what `androidMain/kotlin` held, `src/main/res` what `androidMain/res`
      held, and `src/test/kotlin` what `androidHostTest` held.
- [ ] The `androidResources { enable = true }` block is gone, and the bundled Inter / IBM Plex Mono
      families and the Google sign-in vector still resolve when the app runs.
- [ ] The `ui-tooling` dependency is on `debugImplementation`, not `androidRuntimeClasspath`.
- [ ] `./gradlew qualityGate --rerun-tasks` runs `:ui-android:testDebugUnitTest` — check the task
      list, not just the exit code.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.
