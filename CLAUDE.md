# CLAUDE.md

Per-module guidance lives in each module's own `CLAUDE.md`; Claude loads it automatically when
working in that module.

> **KMP everywhere; each platform owns its UI.** Android renders Compose (`:ui-android`); iOS is a
> native SwiftUI app over the `JustChillKit` framework that `:presentation` exports with SKIE
> ([ADR 005](docs/adr/005-native-swiftui-ios-over-the-kmp-core.md); slice plan in
> `docs/swiftui/PLAN.md`).
>
> Keep the iOS compile (`:presentation:compileKotlinIosSimulatorArm64`, plus `:domain`/`:data`) in
> every gate run — it is the only thing stopping the exported core from silently filling with
> `java.*`. One writer per slice, review inline. Android-only *capabilities* may live in
> `:androidApp`, but their platform-neutral *logic* stays in the KMP core.
> **There are no users on either platform** — see `docs/PROGRESS.md`.

## Build & Development Commands

```bash
# Build
./gradlew assembleDevDebug              # Dev debug build (most common during dev)
./gradlew assembleProdRelease           # Production signed release

# Unit tests (JVM host tests — no device)
./gradlew test                             # Everything
./gradlew :domain:testAndroidHostTest      # also :data:, :presentation:, :ui-android:
./gradlew :androidApp:testDevDebugUnitTest # the MockK ViewModel suite lives here
./gradlew :domain:testAndroidHostTest --tests "com.emm.domain.transaction.CreateTransactionUseCaseTest"

# iOS
./gradlew :presentation:compileKotlinIosSimulatorArm64      # proves zero java.*/android.* leak
./gradlew :presentation:linkDebugFrameworkIosSimulatorArm64 # links JustChillKit + runs SKIE
open iosApp/iosApp.xcodeproj                                # run the SwiftUI app from Xcode
```

KMP host-test tasks go `UP-TO-DATE` across sessions — add `--rerun` to force a real run, and
`--rerun` is **per-task**: with several tasks in one invocation it forces only the task it follows.
There is no `:domain:test` and no `connectedDevDebugAndroidTest`; both died with the KMP migration.

## Project Layout

- Modules in `settings.gradle.kts`: `:androidApp`, `:ui-android`, `:presentation`, `:domain`, `:data`.
- Java toolchain 17 everywhere. `compileSdk = 37`. `minSdk = 28` (`:androidApp`, `:ui-android`,
  `:presentation`) / `26` (`:domain`, `:data`).
- `iosApp/` — Xcode project: SwiftUI app consuming `:presentation`'s `JustChillKit` framework.
  `supabase/` — CLI migrations for the server schema.
- Two product flavors on dimension `tier` (`:androidApp` only):
  - `dev` — `applicationIdSuffix = ".dev"`.
  - `prod` — release signing via `keystore.properties`, Firebase Crashlytics (dev disables
    collection via manifest meta-data; Firebase Analytics is NOT used, only declared in the catalog).

## Architecture

Clean Architecture, five KMP modules. Dependency direction is top to bottom:

| Module | Role | Root package |
|---|---|---|
| `:androidApp` | thin Android entry point (Activity, platform Koin module) | `com.emm.justchill.*` |
| `:ui-android` | Android-only Compose UI (screens, nav, theme) | `com.emm.justchill.{hh.<feature>, core, components}` |
| `:presentation` | compose-free MVI core, ViewModels, Koin DI, formatters; exports `JustChillKit` (SKIE) to iOS | same packages as `:ui-android` on purpose |
| `:data` | implements domain interfaces (commonMain/androidMain/iosMain) | `com.emm.data.<entity>` |
| `:domain` | pure Kotlin, no framework deps | `com.emm.domain.<entity>` |

The iOS SwiftUI app (`iosApp/`) sits on `:presentation` directly; `:ui-android` sits on it as a
Gradle dependency. Same Kotlin packages across the `:ui-android`/`:presentation` boundary — explicit
imports are required where same-package symbols crossed modules.

The app is **local-first**: SQLDelight on-device is the single source of truth and the app is fully
usable with no account and no network. Multi-device sync via Supabase is opt-in and LWW — status in
`docs/sync/PLAN.md`, rationale in `docs/adr/001` and `docs/adr/002`.

`:presentation` depending on `:data` is deliberate: it lets the Koin wiring exist once instead of
per platform. ViewModel purity (VMs take `:domain` interfaces, never SQLDelight or `Default*`
types) is a reviewed convention, not a structural guarantee. What IS structural: ViewModels cannot
touch Compose — `:presentation` has no compose dependency, and the Swift framework would expose
the leak.

### Data flow

`Screen` → `ViewModel` → use case → `Repository` interface → `Default{Entity}Repository` →
`LocalDataSource` (SQLDelight).

### Contracts that span modules

Each module's own CLAUDE.md owns its half; only the chain is documented here.

- **MVI** — `MviViewModel<S, I, E>` plus every ViewModel/UiState/Intent/Effect live in
  `:presentation` (`core/mvi/`); the Screens that consume them live in `:ui-android`.
- **Errors** — sealed `DomainException` (`:domain/shared/error/`) → `:data/shared/SafeCall.kt`
  translates SQLDelight exceptions into it → `:presentation/core/error/DomainExceptionExt.kt`
  turns it into a Spanish message via `toUserMessage()`. Add failure modes by extending
  `DomainException`, never by introducing a new exception type.

## Testing

JUnit4 + MockK + `kotlinx-coroutines-test` as JVM host tests (`androidHostTest`); `:domain` use
cases are the primary surface. Two suites are the ONLY net for their failure mode — a missing Koin
binding (`AppGraphKoinTest`, in `:presentation`) and a missing schema migration (the instrumented
tests in `:data`, not on the default gate). Both are documented where they live.

## Gotchas

- **`./gradlew qualityGate` is the gate.** One definition, in
  `build-logic/.../QualityGateConventionPlugin.kt`; the pre-push hook and all three workflows
  invoke it. detekt over every source set that holds code, the host test suites, dev lint, and
  (on macOS only) the iOS compile. Change the plugin, not the callers.
- **Never gate on plain `./gradlew detekt`** — it is `NO-SOURCE` on all three KMP modules and only
  lints `:androidApp`.
- **Third-party actions in `.github/` are pinned to a commit SHA on purpose** — they hold the
  signing key, the Firebase credentials and the Play service account, and a floating `@v1` can be
  repointed at new code by whoever owns the upstream repo. Do not "tidy" them back into tags;
  dependabot proposes the bumps. GitHub's own `actions/*` stay on tags: trusting them is not an
  extra trust decision, they already own the runner and the secret store.
- **`versionName` is `git describe --match "v[0-9]*"`, and the filter is load-bearing.** The repo
  carries non-release tags (`pre-kmp`, `post-s5`, `pre-redesign`) and a bare `describe` returns the
  nearest one — that is how builds shipped `versionName = "pre-kmp"`. Same filter in `/release`.
- **A tag push does not ship.** `uploadRelease.yml` uploads the AAB to the alpha track as a
  **draft**; publishing it is a manual step in Play Console. A green workflow reached no one.
- `run:` blocks take secrets through `env:`, never `${{ }}` spliced into the script text. Validate
  workflow edits with `actionlint` before pushing — it catches expression and input errors that a
  YAML parse cannot.
- Every route the nav host can push MUST be registered in `NavSavedStateConfiguration.kt`
  (commonMain), else `rememberNavBackStack` crashes on process-death restore. Invisible to the compiler.

## Tooling Versions

Kotlin `2.4.0` · AGP `9.2.1` · Gradle wrapper `9.5.1` · Koin BOM `4.2.1` · SQLDelight `2.3.2` ·
Compose BOM `2026.05.01` · detekt `2.0.0-alpha.3` · Supabase BOM `3.6.0`.

Compose Multiplatform is **gone**: `:ui-android` renders on Google's Compose under the BOM, and the
CMP Gradle plugin is applied nowhere. `:presentation` still uses JetBrains' multiplatform
`lifecycle-viewmodel` — that one has to compile for iOS.

## Open tech debt

- Compose perf pass: audit `derivedStateOf`, `remember`-ed lambdas, `contentType` in `LazyColumn`.
- `SyncOrchestrator` has no connectivity-regained trigger (only on-resume, sign-in, debounced
  writes), so a sync that fails offline waits for the next `ON_RESUME`. No data loss — just latency.
  Fix: `callbackFlow` over `ConnectivityManager.NetworkCallback.onAvailable`, filtered by
  authenticated + (`pendingCount > 0` or `lastSyncFailed`), injected like `resumeEvents`.

## Docs map (`docs/`)

- `PROGRESS.md` — canonical "where are we now". Read it first.
- `adr/` — filenames state the decision; 004 amends 002, 005 supersedes 003's frozen-UI scope.
- `kmp/ORCHESTRATION.md` — slice workflow + ledger. `sync/PLAN.md`, `swiftui/PLAN.md` — slice status.
- `PLAY_ADVERTISING_ID.md` — the app does not use the advertising ID, with the commands that prove
  it on any AAB. Read before answering Play's declaration; the console currently says "Yes", wrongly.
- `DESIGN_SYSTEM.md` — tokens and components (its paths still point at the pre-KMP `app/` module).
- `PRODUCT_*.md`, `ROADMAP_V1.md`, `POST_V1_PLAN.md` — Fases 1-5. `archive/` — closed tracks.

Latest tags: `v2.4.0`, `pre-kmp` (rollback point before the KMP migration). `v2.4.0` is tagged and
built but has NOT reached the alpha track — its Play upload was rejected, see `PLAY_ADVERTISING_ID.md`.
