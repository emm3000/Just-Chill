# E11 — Android only

**Decision:** [ADR 011](../../adr/011-android-only-drop-the-kmp-build-and-the-ios-target.md)

## Why

iOS is dead: ADR 011 deletes the SwiftUI app, the Kotlin/Native targets and SKIE. What replaces the
multiplatform build is a plain Android Kotlin one — three `com.android.library` modules over a
`kotlin("jvm")` `:domain`, one source set each.

## Constraints

- A module that leaves `justchill.kmp.library` must re-register its test task with
  `contributeToQualityGate` — the gate does not fail when it stops naming a suite, it just runs less.
- Conversion order is consumer before dependency (`:ui-android`, `:presentation`, `:data`, `:domain`):
  a KMP `commonMain` cannot resolve a JVM-only or plain-Android artifact.
- `:domain` is `kotlin("jvm")` on purpose — with the iOS compile gone it is the only mechanical thing
  left that stops `android.*` reaching the core.
- `:presentation` carries no Compose dependency. ADR 005 made that a module boundary; after E11 it is
  a reviewed convention and nothing enforces it.
- `multiplatform-settings` stays. It is a KMP-branded library resolving to an Android artifact, and
  swapping it would rewrite where real preference data lives.
- A converted module's detekt baseline splits per variant: one violation now lives in both
  `baseline-<module>-debug.xml` and `-release.xml`, and a burn-down has to shrink both.
- SQLDelight `.sq` and `.sqm` files change path but never name — E02's migration coverage is keyed on
  the file name.
