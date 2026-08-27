# E11-04 — Convert :presentation to a plain Android library

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-03

## Done when

- [ ] `presentation/build.gradle.kts` applies `com.android.library` plus `justchill.detekt` and
      `justchill.quality.gate` directly, and **no** `org.jetbrains.kotlin.android` — AGP 9's
      built-in Kotlin support rejects that plugin outright.
- [ ] `libs.jetbrains.lifecycle.viewmodel` is replaced by the androidx `lifecycle-viewmodel`
      artifact, and the module still declares no Compose dependency.
- [ ] `src/main/kotlin` holds what `commonMain` and `androidMain` held; `src/test/kotlin` holds what
      `commonTest` and `androidHostTest` held.
- [ ] `AppGraphKoinTest` and `TestPlatformModule` still run in the gate — the missing-Koin-binding
      net survives.
- [ ] `./gradlew qualityGate --rerun-tasks` runs `:presentation:testDebugUnitTest`.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.
