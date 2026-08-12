# ADR 003 — iOS is frozen, not closed: keep the compile gate, drop the ceremony

- **Status**: Accepted
- **Date**: 2026-08-08
- **Deciders**: Edgardo Muñoz
- **Amended by**: [ADR 007](007-one-way-of-working-writer-reviewer-and-model-tiers.md), Decision point
  5 (restores writer + reviewer as separate delegated agents, repo-wide — Fact 1 below is stated
  correctly, but this point discounted the maintainer's own device to zero risk, an inference the
  repo corrected on 2026-08-11 in `043cace`).
- **Scope superseded by**: [ADR 005](005-native-swiftui-ios-over-the-kmp-core.md) (frozen UI, thaw
  checklist), which keeps this ADR's compile-gate invariant (point 2) and constraint 8 intact.

> Resumen (es): iOS pasa a estado **congelado**, no cerrado. Se mantiene una sola cosa del ritual —
> `:ui-android:compileKotlinIosSimulatorArm64` en el gate, 12.9s medidos — porque es lo único que impide
> que `commonMain` se llene de `java.*` y convierta un futuro regreso a iOS en un rewrite en vez de un
> "abro Xcode". Se suelta el resto de la ceremonia por slice (writer+reviewer ambos Opus, verificación
> humana en simulador), que estaba calibrada para usuarios en producción que no existen. Se agrega un
> checklist de deshielo para el día que haya un iPhone.

## Context

Three facts, as of 2026-08-08:

1. **There are no users.** Not on Android, not on iOS. The closed Play alpha has no real installs
   beyond the maintainer's own device. This contradicts what `docs/PROGRESS.md` claimed
   ("hay usuarios reales con data en el device desde `4e6de6c`"); that claim is corrected in the same
   commit as this ADR.
2. **The maintainer does not own an iPhone.** The simulator is still runnable — the dev machine is a
   Mac and iOS auth + sync were runtime-validated there against a local Supabase stack. What is
   missing is not the *ability* to verify, it is any *reason* to: no users ask for iOS, and it is not
   a platform the maintainer carries.
3. **The shared base is cheap and clean.** Measured on this trunk:

   | Metric | Value |
   |---|---|
   | `ui-android/commonMain` | 20,119 LOC · 203 files |
   | `ui-android/androidMain` | 155 LOC · 2 files (0.8%) |
   | `ui-android/iosMain` | 376 LOC · 5 files (1.8%) |
   | `expect`/`actual` across the whole repo | 6 declarations |
   | Swift shell | 2 files, ~30 lines |
   | `:ui-android:compileKotlinIosSimulatorArm64 --rerun` | **12.9s** |

Point 3 is the one that reframes the problem. Compose Multiplatform is not the friction — 98% of the
UI is a single codebase and a full Kotlin/Native recompile of it costs thirteen seconds. The friction
is the **ritual built around it**: a nine-task gate, a writer sub-agent plus an adversarial reviewer
sub-agent (both Opus) per slice, and human simulator verification per slice.

That ritual was designed under the belief that real users held real data on real devices. Fact 1
removes that premise. The ceremony is now calibrated for a risk that does not exist.

The maintainer explicitly asked not to close iOS: an iPhone may arrive later.

## Decision

1. **iOS is frozen, not removed.** The `iosArm64` / `iosSimulatorArm64` targets, `iosMain` source
   sets, and `iosApp/` Xcode project all stay in the repository.
2. **`:ui-android:compileKotlinIosSimulatorArm64` stays in every gate run. Non-negotiable.** It is the
   only mechanical guarantee that `commonMain` remains free of `java.*` / `android.*`. Without it,
   `commonMain` drifts JVM-ward silently, and thawing iOS stops being "open Xcode" and becomes a
   migration.
3. **`detektIosMainSourceSet` stays in the pre-push hook** (added in `eb8f034`). While `iosMain` does
   not change, the task is `UP-TO-DATE` and costs effectively nothing.
4. **The 6 `expect`/`actual` declarations are a budget, not a coincidence.** Adding a seventh requires
   a deliberate decision; the default is to hoist the platform bit into a callback the nav host
   supplies, as `ui-android/CLAUDE.md` already prescribes.
5. **Drop the per-slice writer + reviewer (both Opus) requirement.** One writer, reviewed inline.
6. **Drop per-slice human simulator verification.** Frozen means compile-verified only.
7. **Stop asserting iOS parity in docs.** It creates a maintenance obligation that will not be met.
8. **New standing constraint — platform-neutral logic stays in `commonMain`, even for Android-only
   features.** An Android-only *capability* may live in `:androidApp`, but the pure-Kotlin logic
   behind it must not. The first application is the planned notification-capture feature: the
   `NotificationListenerService` is Android-only and belongs in `:androidApp`, but the **parser**
   (regex over notification text, no platform APIs) belongs in `commonMain` with golden tests. If the
   parser is written inside `:androidApp`, iOS can never reuse it; with it in `commonMain`, a thawed
   iOS inherits share-sheet intake driven by the same tested parser. This decision is free today and
   expensive to reverse later.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Remove the iOS targets entirely** | Reverting later costs a migration; keeping them costs 12.9s per gate run. The trade is lopsided in favour of keeping. |
| **Keep full parity discipline** | Calibrated for production users that do not exist. The per-slice ceremony is the measured friction, not the technology. |
| **Freeze *including* the compile gate** | The one thing that must not be dropped. Without it, `commonMain` accumulates JVM-only code invisibly and the thaw becomes a rewrite — which is exactly the outcome "do not close iOS" is meant to prevent. |
| **Publish to TestFlight to keep iOS honest** | Requires an Apple Developer account, real-device verification, and a Google Sign-In implementation that does not exist. Cost without a beneficiary. |

## Consequences

### Positive
- Slice cost drops to one writer and a normal gate; the two-Opus-sub-agent ceremony is gone.
- The option to revive iOS stays open at a measured, bounded price (12.9s per gate run).
- Documentation stops overstating iOS, so future decisions are made against true state.
- Constraint 8 means the platform gap widens in *capabilities* but never in *logic*.

### Negative / costs
- iOS drifts further from verified with every slice. Everything after this ADR is compile-verified
  only — it compiles, and that is the entire claim.
- The thaw checklist grows over time. It must be appended to, not rewritten, as new Android-only
  capabilities land.
- Android-only features (notification capture) permanently widen the capability gap. Constraint 8
  limits the damage but does not eliminate it.
- `compileKotlinIosSimulatorArm64` proves the absence of JVM leakage; it proves nothing about
  runtime. Koin resolution failures on iOS remain invisible to it.

## Thaw checklist — what to do the day an iPhone arrives

Ordered by what fails first and most silently.

1. **Run in the simulator and confirm `initKoin()` resolves.** Koin failures are runtime-only,
   invisible to the compiler *and* to the iOS compile gate (`docs/archive/kmp/ORCHESTRATION.md:99`).
   A static bind trace plus a green compile is not a launch.
2. **Exercise the four modals end to end** — export, import, share, email. They are compile- and
   launch-verified only (`docs/archive/kmp/ORCHESTRATION.md:96`). Watch the UIKit delegate-retention
   landmine (`docs/archive/kmp/ORCHESTRATION.md:91`): UIKit holds a Kotlin/Native `NSObject` delegate by *weak* reference, so
   it is collected mid-flow unless retained; the fix used was a module-level `mutableSetOf<NSObject>()`.
3. **Decide Google Sign-In.** Today it is `UnavailableGoogleSignInLauncher`
   (`ui-android/src/iosMain/.../IosLocalFirstStubs.kt`) and `AuthScreen` hides the button
   (`showGoogleSignIn = false`). Either wire `GIDSignIn` behind the same interface or ship
   email/password only and say so.
4. **Add a `macos-*` CI runner.** All three workflows run on `ubuntu-latest`, which cannot compile
   Kotlin/Native for iOS — so today the gate's most important invariant is verified only on the
   maintainer's laptop.
5. **Re-verify sync against a real Supabase project.** The existing validation ran against a local
   stack; the cloud prod project does not exist yet (see `docs/PROGRESS.md`, alpha blockers).
6. **Re-check `startTab`.** `PlatformHostActions.ios.kt` sets `HomeRoute` while Android sets
   `SeeTransactionRoute` — a divergence that should be resolved as part of the navigation rework, not
   preserved by accident.

## Notes

- This ADR does not amend ADR 001 or ADR 002; the local-first architecture and the sync cursor are
  unaffected.
- `docs/archive/kmp/ORCHESTRATION.md` keeps its ledger and landmines, and `docs/WORKFLOW.md` keeps the
  reinforced-gate definition, both of which stay accurate and useful. What this ADR retires is the
  per-slice *sub-agent protocol* around that gate, not the gate itself.
- Reversing this ADR is cheap in the direction of more rigour: reinstating the ceremony is a docs
  change. That asymmetry is deliberate.
