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
- [ ] `ui-android/gradle.properties` is deleted — its only content is the `justchill.kmp.ios`
      opt-out the plugin no longer reads.
- [ ] `E01-18-render-disclosurepending-on-ios.md` is `git rm`'d: it renders a SKIE-exported case
      on a screen that will never exist.
- [ ] `docs/swiftui/PLAN.md` is in `docs/archive/swiftui/` and
      `git grep -l 'docs/swiftui' -- ':!docs/archive' ':!docs/adr' ':!docs/work/doing/E11-01-*'`
      returns nothing.
- [ ] `trunk`'s branch-protection required status checks name no `ios-compile` context. Nothing in
      the tree records that context, so no grep and no gate task can catch this — check it by hand
      (`gh api repos/<owner>/<repo>/branches/trunk/protection --jq '.required_status_checks.contexts'`)
      and remove it there if it still appears.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.

## Context

The modules stay KMP through this ticket — only the iOS target leaves. `libs.versions.toml` still
declares `skie` and the multiplatform plugins; E11-07 purges them.
