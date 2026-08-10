# Native iOS (SwiftUI) over the KMP core — execution plan

> Learning-driven track: build a native SwiftUI iOS app on top of the existing
> KMP core (`:domain` + `:data` + extracted `:presentation`), feature by
> feature, while the Android app stays untouched and shippable at every step.
> Compose Multiplatform stays as Android's UI; the CMP-on-iOS entry point is
> retired early in the track. Needs an ADR (005) formalizing the reversal of
> the "one shared UI" direction and superseding the scope of
> [ADR 003](../adr/003-freeze-ios-keep-the-compile-gate.md) (the compile gate
> survives, but moves to `:presentation`).
>
> Motivation is explicit: **practice SwiftUI and KMP↔Swift interop (SKIE)
> against a real app**. There are no users on either platform; risk is zero.
> If motivation dies mid-track, every slice leaves trunk green and Android
> whole — abandoning halfway costs nothing.

Written 2026-08-10 from live recon of trunk @ `7ae6cda`. Numbers below are
from that commit.

## Starting point

- `shared-ui/commonMain` = 21,050 lines: ~2,503 portable presentation
  (ViewModels + UiState/Intent/Effect + MVI core), ~18,000 Compose UI,
  510 Koin DI (14 modules in `hh/di/`).
- `:domain` and `:data` are already pure KMP. **Zero work there.**
- ViewModels are compose-free (verified by import scan). The ONLY compose
  coupling in the portable layer: `@Stable` / `@Immutable`
  (`androidx.compose.runtime`) annotations on 11 UiState files.
- `core/error`, `core/format`, `core/preferences`, `core/sync`,
  `hh/shared/UiStrings.kt`: compose-free (verified).
- `MviViewModel` extends multiplatform `androidx.lifecycle.ViewModel` +
  `viewModelScope`; effects via `Channel.receiveAsFlow()`. Works on
  iOS targets; lifecycle on iOS is manual (see VM-bridge pattern, S3).
- iOS framework today: `binaries.framework { baseName = "Shared" }` declared
  in `shared-ui/build.gradle.kts`, consumed by `iosApp/` (`iOSApp.swift` +
  `ContentView.swift` hosting the CMP controller).
- Convention plugin `justchill.kmp.library` (build-logic) gives any new
  module android + iosArm64 + iosSimulatorArm64 + detekt + qualityGate wiring.

## Target architecture

```
:domain          (unchanged)
:data            (unchanged)
:presentation    NEW — KMP: MVI core, all VMs/State/Intent/Effect, Koin DI,
                 UiStrings, formatters, error mapping, preferences, sync port.
                 Declares the iOS framework (baseName "JustChillKit",
                 export :domain + :data) + SKIE.
:shared-ui       Compose UI only. Depends on :presentation. Drops iOS targets
                 in S2 → becomes Android-only.
:androidApp      unchanged (thin shell)
iosApp/          SwiftUI app consuming JustChillKit. CMP entry retired in S2.
```

Kotlin packages do NOT change when files move to `:presentation` — same
`com.emm.justchill.*` packages, different module. No import churn in
`:shared-ui` screens or `:androidApp` tests.

## Decisions locked up front

1. **Strip `@Stable`/`@Immutable` from UiState** instead of adding
   compose-runtime to `:presentation`. A compose dep in the framework module
   drags the compose runtime klib into the iOS binary for two annotations.
   Compose compiler in `:shared-ui` then sees cross-module classes without
   annotations → treats them as unstable → gratuitous recomposition. Fix:
   `compose.stabilityConfigurationFile` in `:shared-ui` declaring
   `com.emm.justchill.**` state classes stable. Verify with compose compiler
   metrics if a screen feels off afterwards.
2. **The framework is declared by `:presentation`, never `:shared-ui`.**
   Exporting shared-ui would embed the Compose/Skia runtime in the binary.
3. **SKIE is non-negotiable** for sealed→Swift enums (exhaustive `onEnum(of:)`)
   and Flow→AsyncSequence. Raw Kotlin/Native interop for this MVI surface is
   not worth learning around. Check the SKIE↔Kotlin 2.4.0 compatibility
   matrix BEFORE starting S2 — SKIE tracks Kotlin releases with some lag.
4. **VM lifecycle on iOS is manual.** SwiftUI wrapper (`@Observable` bridge)
   owns the VM: spawns a `Task` per `for await` on `state`/`effect`, calls
   `vm.clear()` on deinit. Pattern defined once in S3, reused everywhere.
5. **Strings shared, theme duplicated.** `UiStrings.kt` (plain Kotlin, already
   Spanish) travels through the framework — one copy source. Visual tokens do
   NOT: `Theme.swift` re-declares colors/typography natively. The design
   system is per-platform by design; SwiftUI should feel like iOS, not like a
   port.
6. **Result channels die with nav3 on iOS.** `pendingCategory` /
   `pendingImportJson` are nav3-cache workarounds (see the `() -> T?` landmine
   in AppNavHost). SwiftUI verticals use plain closures/bindings — do not
   port the mechanism.

## Slices

Every slice ends with: Android gate green (see "Gate per slice"), iOS app
compiles and runs, trunk shippable. One slice ≈ 1-3 side-project sessions.

| # | Slice | Content | SwiftUI/interop skill it teaches |
|---|-------|---------|----------------------------------|
| S1 | Extract `:presentation` | Move: `core/mvi`, `core/error`, `core/format`, `core/preferences`, `core/sync`, per-feature VMs + State + Intent + Effect + `toUi` mappers + `SelectableCategory`, all 14 `hh/di` modules, `AppGraph.kt`, `UiStrings.kt`, formatters. Strip stability annotations + stability config file in shared-ui. Move `AppGraphKoinTest` + `MviViewModelTest` + VM host tests to `:presentation` androidHostTest. `:shared-ui` gets `api(project(":presentation"))`. | None yet — pure Gradle/KMP surgery. |
| S2 | Framework + SKIE + Swift bootstrap | `:presentation` declares `JustChillKit` framework (export domain+data) + SKIE plugin. Move `KoinIos.kt`, `IosLocalFirstStubs.kt` (review what it stubs), `PrintlnSyncLogger.kt`, iOS platform module → `presentation/iosMain`. Replace `ContentView.swift` CMP host with a bare SwiftUI `App` that starts Koin, resolves one VM, renders its state as `Text`. `shared-ui` drops iOS targets; gate's iOS leg moves to `:presentation:compileKotlinIosSimulatorArm64`. Write ADR 005. | Xcode target setup, embedAndSign, SKIE codegen, Koin from Swift. |
| S3 | First vertical: SeeTransactions (read-only) | `NavigationStack` skeleton, minimal `Theme.swift`, the **VM-bridge pattern** (decision 4), transaction list + search + date filters. | The pattern everything else reuses: StateFlow→`@Observable`, intents, effect collection, List, searchable. |
| S4 | Home + tabs | `TabView` with the bottom-bar tabs, home summary screen, tab-switch navigation (no nav3 `switchTab` semantics — native TabView state). | TabView, per-tab NavigationStack, formatters through the framework. |
| S5 | Transaction add/edit | Forms, decimal input/keyboard, date picker, category selection via closure (decision 6), edit flow. | Form, sheets, focus/keyboard management, returning results without a result channel. |
| S6 | Categories CRUD | List + add/edit + delete with confirmation copy (`DeleteCategoryCopyTest` strings via framework). | Swipe actions, confirmation dialogs. |
| S7 | Recurring movements | List + add/edit vertical. | Consolidation — same patterns, less hand-holding. |
| S8 | Reports | Charts + period selection + share. | **Swift Charts**, `ShareLink`/`UIActivityViewController`. |
| S9 | Profile + backup | Export/import via `fileExporter`/`fileImporter` (replaces SAF flow; destructive-replace confirmation dialog), delete account, privacy policy link, app version. | File pickers, destructive flows, `Link`. |
| S10 | Auth + sync UI | Supabase email/password + Google Sign-In (native iOS SDK) + claim-on-sign-in + sync status/snackbar equivalents. `ResumeEvents` iOS actual already exists (`NSNotificationCenter`). | Third-party SDK integration, async auth flows, app-lifecycle events. |
| S11 | Onboarding + closure | Manifesto first-launch gate (`AppPreferences` shared), parity audit vs Android, delete CMP leftovers, finalize ADR 005, update `PROGRESS.md` + `ORCHESTRATION.md` + module CLAUDE.md files. | Scene phases, state restoration audit. |

Order rationale: read-only vertical first (pattern with least surface), forms
second (most reused skill), platform APIs (files/charts/share) third, external
SDK auth second-to-last (heaviest integration), onboarding last (trivial UI,
needs everything else for the parity audit).

## Gate per slice

- `./gradlew qualityGate` — **beware `UP-TO-DATE` lies**: `--rerun` is a task
  option, not a build flag; with several tasks it applies ONLY to the task it
  follows. Repeat it per task or run tasks separately when you need proof of
  a real run.
- `./gradlew assembleDevDebug` + forced host suites (`:presentation` joins
  via the convention plugin).
- `:presentation:compileKotlinIosSimulatorArm64` (replaces the shared-ui leg
  from S2 on).
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp build` for the
  SwiftUI app (manual from Xcode is fine; automating it into the macOS gate
  leg is optional, decide when it hurts).

## Risks / open items

- **SKIE ↔ Kotlin 2.4.0 compatibility** — verify before S2; blocking if
  unsupported (wait or pin Kotlin).
- **Stability regression on Android** after stripping annotations — mitigated
  by the stability configuration file; verify with compiler metrics if a
  list screen starts stuttering.
- **`IosLocalFirstStubs.kt` contents unknown** — review in S2 what is stubbed
  (DB driver? auth?) and what must become real for verticals to work on
  device.
- **VM host tests moving modules** (S1) — the androidApp MockK suite
  (`testDevDebugUnitTest`) stays put and keeps passing (packages unchanged);
  only shared-ui host tests move. If a test needs MockK it stays in an
  `androidHostTest` source set, never commonTest.
- **Motivation** — the real risk of a side project. Every slice ships a
  visible screen; never two slices of pure plumbing after S2.
