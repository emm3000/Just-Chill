# E11-01 — Delete the iOS app and its build plumbing

**Epic:** [E11 — Android only](../epics/E11-android-only.md)

## Done when

- [ ] `iosApp/` no longer exists.
- [ ] `data/src/iosMain` and `presentation/src/iosMain` no longer exist.
- [ ] `KmpLibraryConventionPlugin` declares no `iosArm64` or `iosSimulatorArm64` target and reads no
      `justchill.kmp.ios` property.
- [ ] `presentation/build.gradle.kts` applies neither `libs.plugins.skie` nor
      `justchill.ios.supabase.config`, and declares no `binaries.framework` block.
- [ ] `IosSupabaseConfigConventionPlugin.kt` and `GenerateIosSupabaseConfigTask.kt` are deleted and
      `build-logic/build.gradle.kts` registers neither.
- [ ] `QualityGateConventionPlugin` names no `detektIosMainSourceSet` and matches no
      `compileKotlinIos` task.
- [ ] `docs/swiftui/PLAN.md` is in `docs/archive/` and `git grep -l 'docs/swiftui' -- ':!docs/archive'`
      returns nothing.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.

## Context

The modules stay KMP through this ticket — only the iOS target leaves. `libs.versions.toml` still
declares `skie` and the multiplatform plugins; E11-07 purges them.
