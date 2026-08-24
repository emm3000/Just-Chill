# Native iOS (SwiftUI) over the KMP core — execution plan

> Learning-driven track: a native SwiftUI app on top of `:presentation`
> (`JustChillKit` + SKIE), feature by feature, while Android stays untouched
> and shippable at every step. Android renders its own Compose UI, never a
> shared one — ADR 005 formalizes the reversal of the "one shared UI"
> direction and supersedes the scope of
> [ADR 003](../adr/003-freeze-ios-keep-the-compile-gate.md) (the compile gate
> survives, on `:presentation`).
>
> Motivation is explicit: **practice SwiftUI and KMP↔Swift interop against a
> real app**. There are no users on either platform. Every slice leaves trunk
> green and Android whole, so abandoning halfway costs nothing.

## Target architecture

```
:domain          pure Kotlin
:data            implements the domain ports
:presentation    MVI core, all VMs/State/Intent/Effect, Koin DI, UiStrings,
                 formatters, error mapping, preferences.
                 Declares the JustChillKit framework + SKIE.
:ui-android      Android-only Compose UI. Depends on :presentation.
:androidApp      thin shell.
iosApp/          SwiftUI app consuming JustChillKit.
```

Kotlin packages are the same across `:presentation` and `:ui-android` on
purpose; same-package symbols crossing that Gradle boundary still need
explicit imports.

## Decisions locked up front

1. **The framework is declared by `:presentation`, never `:ui-android`.**
   Exporting `:ui-android` would embed the Compose/Skia runtime in the binary.
2. **No compose-runtime in `:presentation`.** UiState carries no
   `@Stable`/`@Immutable`; `:ui-android` declares those classes stable through
   `compose_stability.conf` instead. Without it the Compose compiler treats
   cross-module state as unstable and recomposes gratuitously. Verify with
   compiler metrics if a list screen starts stuttering.
3. **SKIE is non-negotiable** for sealed→Swift enums (exhaustive `onEnum(of:)`)
   and Flow→AsyncSequence. Raw Kotlin/Native interop for this MVI surface is
   not worth learning around. SKIE tracks Kotlin releases with lag — check the
   compatibility matrix before bumping either.
4. **VM lifecycle on iOS is manual.** A SwiftUI `@Observable` wrapper owns the
   VM: one `Task` per `for await` on `state`/`effect`, `vm.clear()` on deinit.
   The pattern is defined once in S3 and reused everywhere.
5. **Strings shared, theme duplicated.** `UiStrings.kt` travels through the
   framework — one copy source. Visual tokens do not: `Theme.swift` re-declares
   colors and typography natively. SwiftUI should feel like iOS, not like a port.
6. **Result channels die with nav3 on iOS.** `pendingCategory` /
   `pendingImportJson` are nav3-cache workarounds (see the `() -> T?` landmine
   in `AppNavHost`). SwiftUI verticals use plain closures and bindings — do not
   port the mechanism.

## Slices

Every slice ends with: Android gate green, iOS app compiles and runs, trunk
shippable. One slice ≈ 1-3 side-project sessions.

| # | Slice | Content | SwiftUI/interop skill it teaches |
|---|-------|---------|----------------------------------|
| S3 | First vertical: SeeTransactions (read-only) | `NavigationStack` skeleton, minimal `Theme.swift`, the **VM-bridge pattern** (decision 4), transaction list + search + date filters. | The pattern everything else reuses: StateFlow→`@Observable`, intents, effect collection, List, searchable. |
| S4 | Tab shell | `TabView` over the four tabs Android now ships — Ver, Reporte, Cuentas, Perfil — plus the centre add button, and tab-switch navigation (no nav3 `switchTab` semantics — native TabView state). There is no home summary screen to port: Android deleted it. Reporte's own content is S8; this slice builds only the shell. | TabView, per-tab NavigationStack, formatters through the framework. |
| S5 | Transaction add/edit | Forms, decimal input/keyboard, date picker, category selection via closure (decision 6), edit flow. | Form, sheets, focus/keyboard management, returning results without a result channel. |
| S6 | Categories CRUD | List + add/edit + delete with confirmation copy (`DeleteCategoryCopy` strings via framework). | Swipe actions, confirmation dialogs. |
| S7 | Recurring movements | List + add/edit vertical. | Consolidation — same patterns, less hand-holding. |
| S8 | Reports | Charts + period selection + share, including the Tendencias tab. | **Swift Charts**, `ShareLink`/`UIActivityViewController`. |
| S9 | Profile + manual backup | Manual export/import via `fileExporter`/`fileImporter` (replaces the SAF flow; keep the destructive-replace confirmation), delete account, privacy policy link, app version. | File pickers, destructive flows, `Link`. |
| S10 | Auth + snapshot backup UI | Supabase email/password sign-in, plus native Google Sign-In replacing `UnavailableGoogleSignInLauncher` (`presentation/iosMain`). Drive `BackupOrchestrator` off the iOS lifecycle — `ResumeEvents.ios.kt` already exists — and render backup status/staleness. Read `docs/work/epics/E01-snapshot-backup.md` first. | Third-party SDK integration, async auth flows, app-lifecycle events. |
| S11 | Onboarding + closure | Manifesto first-launch gate (`AppPreferences` shared), parity audit vs Android, finalize ADR 005, update `PROGRESS.md` + `WORKFLOW.md` + module CLAUDE.md files. | Scene phases, state restoration audit. |

Order rationale: read-only vertical first (least surface), forms second (most
reused skill), platform APIs (files/charts/share) third, external SDK auth
second-to-last (heaviest integration), onboarding last (trivial UI, needs
everything else for the parity audit).

## Gate per slice

- `./gradlew qualityGate` — **beware `UP-TO-DATE` lies**: `--rerun` is a task
  option, not a build flag; with several tasks it applies ONLY to the task it
  follows. Repeat it per task, or run tasks separately, when you need proof of
  a real run.
- `:presentation:compileKotlinIosSimulatorArm64` — the only thing stopping the
  exported core from silently filling with `java.*`.
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp build` for the
  SwiftUI app. Manual from Xcode is fine; automating it into the macOS gate leg
  is optional — decide when it hurts.

## Risks / open items

- **Session storage on iOS.** `KoinIos.kt` puts the Supabase session in
  `NSUserDefaults` — unencrypted and inside the iCloud backup. A refresh token
  belongs in the Keychain, which needs a real `SessionManager`, not a
  `SettingsSessionManager`. Blocking for S10.
- **Stability regression on Android** if `compose_stability.conf` drifts from
  the state classes it names. Nothing fails the build when it does.
- **Motivation** — the real risk of a side project. Every slice ships a visible
  screen; never two slices of pure plumbing.
