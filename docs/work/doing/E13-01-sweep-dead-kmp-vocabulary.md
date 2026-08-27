# E13-01 — Sweep dead KMP vocabulary

**Epic:** [E13 — Dead vocabulary](../epics/E13-dead-vocabulary.md)
**Blocked by:** —

## Done when

- [ ] `rg -in 'multiplatform|kmp|commonMain|androidMain|iosMain|commonTest|androidHostTest|androidDeviceTest' --glob '!docs/**' --glob '!.git/**' .`
      returns only: real `multiplatform-settings`/`multiplatform.settings` dependency coordinates
      and their comments (`gradle/libs.versions.toml`, `androidApp/build.gradle.kts`,
      `presentation/build.gradle.kts`, `TestPlatformModule.kt`, `AppGraphKoinTest.kt`);
      `config/detekt/detekt.yml`'s functional `excludes`/`multiplatformTargets` YAML; and the
      historical, load-bearing comments E13 names as the exception (`pre-kmp` in root `CLAUDE.md`
      and `androidApp/build.gradle.kts`, ADR 011's dropped detekt task names in
      `QualityGateConventionPlugin.kt`, the KMP-stranded-source-set gotcha and the KMP publishing
      gotcha in `data/build.gradle.kts`, the Compose Multiplatform → nav3 note in
      `libs.versions.toml`, the Compose Multiplatform note in `ui-android/build.gradle.kts`, and
      `domain/CLAUDE.md`'s ADR 011 line).
- [ ] No `*ViewModel*.kt` file or `presentation/src/main/kotlin/com/emm/justchill/core/mvi/**` file
      is touched.
- [ ] No line of executable Kotlin, Gradle DSL or YAML config changed — comments and KDoc only.

## Context

ADR 011 converted the build; the comments describing `commonMain`/`androidMain`/`iosMain` source
sets did not. Most `hh/di/*.kt` header comments narrate a migration nobody will repeat and point at
a binding one `rg` away — delete those. A few carry a live constraint under dead vocabulary —
rewrite those to the constraint. See `docs/work/epics/E13-dead-vocabulary.md` for the KEEP test.
