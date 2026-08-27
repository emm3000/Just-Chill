# ADR 011 — Android-only: drop the KMP build and the iOS target

- **Status**: Accepted
- **Date**: 2026-08-26
- **Deciders**: Edgardo Muñoz
- **Supersedes**: [ADR 005](005-native-swiftui-ios-over-the-kmp-core.md) **in full** — the SwiftUI
  track, `JustChillKit`, SKIE and the relocated compile gate all end here.
- **Scope superseded**: [ADR 003](003-freeze-ios-keep-the-compile-gate.md)'s one surviving
  invariant — `compileKotlinIos*` on every gate run, the only mechanical proof that the exported
  core stays free of `java.*` / `android.*`. It is removed. Decision 3 states what replaces it and
  Decision 4 states what it does not.

> Resumen (es): JustChill deja de ser Kotlin Multiplatform y pasa a ser un proyecto Kotlin/Android a
> secas. Se borran `iosApp/`, los targets de Kotlin/Native, SKIE y el framework `JustChillKit`. La
> única motivación que ADR 005 declaró fue **aprender** (SwiftUI + interop KMP↔Swift); esa meta ya no
> está, y nunca hubo usuarios iOS ni iPhone. Lo que pagas hoy no son los 9 archivos de `iosMain`: son
> ≈610 archivos repartidos entre `commonMain`, `androidMain`, `commonTest` y `androidHostTest` en vez
> de `src/main` y `src/test`. Asume el precio antes de seguir: si algún día quieres iOS otra vez, no
> te alcanza con abrir Xcode — reescribes `:data` y `:presentation`. ADR 003 mantuvo el gate de
> compile justamente para evitar eso, y aquí lo quitas a sabiendas.

## Context

1. **ADR 005's motivation was learning, not a product need** — SwiftUI plus KMP↔Swift interop
   through SKIE, practised against a real codebase. ADR 003 Facts 1 and 2 are both still true: there
   are no iOS users, and the maintainer owns no iPhone. The learning goal has been dropped, and
   nothing else was holding the track up.
2. **The iOS app never left hello world.** `iosApp/` is 41 lines of Swift across `ContentView.swift`
   and `iOSApp.swift`. Every slice past S2 in `docs/swiftui/PLAN.md` was never started.
3. **The multiplatform surface being paid for is small**: 9 `.kt` files under `iosMain` across
   `:data` and `:presentation`, and 6 `expect`/`actual` pairs — `Dispatchers` and `SqliteExceptions`
   in `:data`, `BackgroundEvents` and `ResumeEvents` in `:presentation`.
4. **The cost is the layout, not those files.** ≈610 `.kt`/`.sq`/`.sqm` files sit under
   `commonMain`, `androidMain`, `commonTest` and `androidHostTest` instead of `src/main` and
   `src/test`, and every library module goes through `KmpLibraryConventionPlugin` and AGP's
   `com.android.kotlin.multiplatform.library` rather than `com.android.library`.
5. **Compose Multiplatform was already gone before this ADR.** The only `org.jetbrains` Compose entry
   left in `gradle/libs.versions.toml` is `org.jetbrains.kotlin.plugin.compose`, which is the Kotlin
   compiler plugin for Google's Compose. This ADR does not touch it.

## Decision

1. **Delete the iOS half.** `iosApp/`, both `iosMain` source sets, the `iosArm64` /
   `iosSimulatorArm64` targets, the SKIE plugin, the `JustChillKit` framework block, and the
   `justchill.ios.supabase.config` plugin with its `GenerateIosSupabaseConfigTask`.
2. **`:ui-android`, `:presentation` and `:data` become `com.android.library` +
   `org.jetbrains.kotlin.android`**, with one `src/main` and one `src/test` each. `:data` keeps
   `src/androidTest` for the device migration suite.
3. **`:domain` becomes `kotlin("jvm")` — no Android plugin.** This is what replaces the iOS compile
   gate: with no Android artifact on `:domain`'s compile classpath, `android.*` and `androidx.*` stop
   resolving there. That is a stronger guarantee than compiling for Kotlin/Native was — it is the
   dependency graph refusing, not a second compilation happening to pass.
4. **What is not replaced.** `:presentation` needs `androidx.lifecycle.ViewModel`, so it must stay an
   Android library. Its compose-free purity drops from a module boundary to a **reviewed
   convention**: after this ADR nothing mechanical rejects a Compose import in a ViewModel.
5. **`multiplatform-settings` (`com.russhwolf`) stays.** It is a KMP-branded library that resolves to
   a working Android artifact; replacing it would rewrite where real preference data lives, and this
   ADR buys nothing by doing that.
6. **The conversion order is fixed, and it is consumer before dependency**: `:ui-android`, then
   `:presentation`, then `:data`, then `:domain`. A KMP `commonMain` cannot resolve a JVM-only or
   plain-Android artifact, so converting `:domain` first breaks `:data`'s `commonMain`.

## Consequences

### Positive

- One source set per module, no `expect`/`actual`, no SKIE, no Kotlin/Native toolchain, no Xcode
  project.
- `qualityGate` loses its `compileKotlinIos*` leg and `detektIosMainSourceSet` with it, and stops
  being conditional on a macOS host.
- androidx artifacts replace the JetBrains multiplatform ports —
  `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` becomes `androidx.lifecycle:lifecycle-viewmodel`.
- Kotlin upgrades stop waiting on SKIE's support window, a cost ADR 005 booked explicitly.

### Negative / costs

- **Returning to iOS is now a rewrite of `:data` and `:presentation`, not "I open Xcode."** ADR 003
  kept the compile gate for exactly this reason and priced removal as "reverting later costs a
  migration". That is the thing being spent here. The maintainer accepted it; it is not mitigated
  and it does not come back cheaply.
- `:presentation` purity is now unenforced — see Decision 4. The Swift consumer that ADR 005 counted
  as the enforcement mechanism is gone with it.
- **The trap this creates for the next writer**: `KmpLibraryConventionPlugin` calls
  `contributeToQualityGate("testAndroidHostTest")`. A module that leaves that plugin without
  re-registering its new test task (`testDebugUnitTest`) **silently stops being tested by the gate**.
  The gate does not fail — it just runs less. Verify the gate's task list at each module's
  conversion, not once at the end.

## Notes

- `docs/swiftui/PLAN.md` moves to `docs/archive/`.
- ADR 003's standing constraint 8 (platform-neutral logic stays out of `:androidApp`) loses the
  beneficiary that justified it. It survives as module hygiene, nothing more.
- The work was tracked as epic E11, closed and archived at
  [`docs/archive/E11-android-only.md`](../archive/E11-android-only.md).
