# ADR 005 — Native SwiftUI on iOS over the KMP core; CMP stays Android's UI

- **Status**: Accepted
- **Date**: 2026-08-10
- **Deciders**: Edgardo Muñoz

> Resumen (es): iOS se descongela — pero no para volver a Compose Multiplatform, sino como app
> **SwiftUI nativa** sobre el core KMP (`:domain` + `:data` + `:presentation`, exportados como el
> framework `JustChillKit` con SKIE). La motivación es explícita: **aprendizaje** (SwiftUI + interop
> KMP↔Swift contra una app real, con cero usuarios y cero riesgo). Compose Multiplatform sigue
> siendo la UI de Android sin cambios; lo que se retira es el entry point CMP de iOS. El gate de
> compile de iOS sobrevive — se muda de `:shared-ui` a `:presentation`.

## Context

ADR 003 froze iOS at "it compiles" because there were no users and no reason to verify further.
Both facts still hold. What changed is the maintainer's goal: practicing SwiftUI and KMP↔Swift
interop (SKIE) against a real codebase — charts, document pickers, auth, sync — is worth more than
any tutorial project, and this repo is the best vehicle available for it.

Slice S1 (`3a043d2`) made the move cheap to start: the compose-free presentation layer
(MVI core, every ViewModel/UiState/Intent/Effect, Koin DI, formatters, strings) now lives in
`:presentation`, a module with no Compose dependency — which restored the structural guarantee that
ViewModels never touch Compose, and made the module exportable to Swift. The full slice plan lives
in `docs/swiftui/PLAN.md`.

## Decision

1. **iOS is unfrozen as a learning track, not a product commitment.** There are still no users;
   nothing here promises parity or a release.
2. **iOS UI is native SwiftUI consuming `JustChillKit`** — the framework `:presentation` declares
   (static, SKIE-processed, exporting `:domain` + `:data`). The CMP-on-iOS entry
   (`MainViewController`, `ComposeView`) is deleted, not preserved.
3. **Android keeps Compose Multiplatform unchanged.** `:shared-ui` becomes Android-only
   (`justchill.kmp.ios=false`); nothing about the Android app's architecture, gate, or behavior
   changes.
4. **The compile gate survives, relocated.** ADR 003's non-negotiable —
   `compileKotlinIosSimulatorArm64` on every gate run — now runs through `:domain`, `:data` and
   `:presentation`. The invariant it protects is the same: the exported core stays free of
   `java.*`/`android.*`.
5. **SKIE is part of the contract.** Sealed hierarchies, `Flow`/`StateFlow` and suspend functions
   cross the boundary as Swift enums, `AsyncSequence` and `async` functions. Raw Kotlin/Native
   interop for this MVI surface was rejected.
6. **ADR 003's standing constraint 8 stays**: platform-neutral logic lives in the KMP core even for
   Android-only features. It matters *more* now — a SwiftUI iOS app reuses exactly what lives below
   `:shared-ui`.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Stay frozen (ADR 003 as-is)** | The freeze was calibrated against "no reason to touch iOS". A concrete learning goal is a reason; the freeze has no users to protect. |
| **Unfreeze on Compose Multiplatform** | Practices nothing new — CMP on iOS is the same Compose code Android already uses. The goal is SwiftUI and interop, not pixel parity. |
| **Greenfield SwiftUI toy app** | Faster SwiftUI reps, but skips the valuable half: consuming a real KMP core (SKIE, Koin from Swift, StateFlow bridging) — the production-dominant architecture. |
| **Raw Kotlin/Native interop (no SKIE)** | Sealed types become opaque classes, flows become callback plumbing, cancellation is manual. The learning value drowns in boilerplate. |

## Consequences

### Positive
- Every slice of `docs/swiftui/PLAN.md` leaves trunk green and Android untouched; abandoning the
  track at any point costs nothing.
- The iOS gate leg shrinks: `:presentation` compiles ~2.5k lines instead of `:shared-ui`'s ~21k.
- `:presentation` being compose-free is now enforced by the Swift consumer, not just the module
  graph — a Compose type in a ViewModel's signature would surface in the framework's interface.

### Negative / costs
- The SwiftUI app rebuilds ~18k lines of UI by hand over many sessions; motivation is the real
  risk (mitigated by shipping one visible vertical per slice).
- Two UIs mean divergence in *presentation behavior* is now possible per platform; the shared
  ViewModels bound what can diverge, but copy, layout and navigation are per-platform by design.
- SKIE couples the build to its Kotlin-version support window (0.10.14 ↔ Kotlin 2.0.0–2.4.10);
  Kotlin upgrades now wait for SKIE.
- ADR 003's thaw checklist is superseded for UI concerns (the modals, `startTab`) — those flows are
  being rebuilt natively — but items 1 (Koin runtime resolution on device), 4 (macOS CI runner) and
  5 (real Supabase project) remain live and unowned.

## Notes

- Supersedes the *scope* of ADR 003 (frozen UI, thaw checklist), keeps its compile-gate invariant
  and its constraint 8. ADR 001/002/004 (local-first, sync cursor, conflict arbitration) are
  unaffected — the sync engine ships to iOS inside `JustChillKit` unchanged.
- The operational plan, slice ledger and per-slice gate live in `docs/swiftui/PLAN.md`.
