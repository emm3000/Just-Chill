# E11 — Android only

**Decision:** [ADR 011](../../adr/011-android-only-drop-the-kmp-build-and-the-ios-target.md)

## Why

iOS is dead: ADR 011 deletes the SwiftUI app, the Kotlin/Native targets and SKIE. What replaces the
multiplatform build is a plain Android Kotlin one — three `com.android.library` modules over a
`kotlin("jvm")` `:domain`, one source set each.

## Constraints

- `:domain` is `kotlin("jvm")` on purpose — it is the only mechanical thing that stops `android.*`
  reaching the core, and the root `CLAUDE.md`'s gate gotcha owns the per-module test-task
  registration that replaced `contributeToQualityGate`.
- `:presentation` carries no Compose dependency. ADR 005 made that a module boundary; after E11 it is
  a reviewed convention and nothing enforces it.
- `multiplatform-settings` stays. It is a KMP-branded library resolving to an Android artifact, and
  swapping it would rewrite where real preference data lives.
- A converted module's detekt baseline splits per variant: one violation now lives in both
  `baseline-<module>-debug.xml` and `-release.xml`, and a burn-down has to shrink both.
- SQLDelight `.sq` and `.sqm` files change path but never name — E02's migration coverage is keyed on
  the file name.
- `:domain`'s `kotlin-jvm` catalog alias is unversioned on purpose — build-logic's `kotlin-dsl` plugin
  embeds a Kotlin older than the project's, so `build-logic/build.gradle.kts` pins the real
  kotlin-gradle-plugin jar explicitly instead. Drop that pin and `:domain` compiles against a stdlib a
  minor version behind: the gate goes red, but on opt-in errors in the source, naming nothing about
  the classpath. `:domain:dependencies --configuration compileClasspath` is what answers it.
