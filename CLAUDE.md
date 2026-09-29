# CLAUDE.md

Operating manifest for this repo. Loaded in every session.

## Mandatory state

The product is a **local-first** personal finance app. SQLDelight on device is the source of truth for reads and writes; the app is fully usable with no account and no network. Supabase exists for one thing, the opt-in **snapshot backup** (ADR 009): no row replication, no multi-device convergence. The sync engine is gone and is not coming back; its schema stays.

No third-party users, but **the author runs the release daily on a device holding real accumulated data**. UI is cheap to redo; a destructive migration is not.

## Modules

```
androidApp        -> shared, feature:*, core:backup, core:database, core:ui, core:presentation, core:domain
shared            -> feature:*, core:backup, core:database, core:presentation, core:domain
feature:*         -> core:ui, core:presentation, core:domain, core:testing
core:backup       -> core:domain
core:database     -> core:domain
core:ui           -> core:presentation, core:domain
core:presentation -> core:domain
core:testing      -> core:domain
```

- `:core:domain` — **pure Kotlin**, KMP `commonMain` (`justchill.kmp.library`, its tests in `androidHostTest`): models, value objects, use cases and the repository interfaces. `kotlinx-coroutines-core` and `kotlinx-datetime` only.
- `:core:database` — the domain interfaces implemented, KMP (`justchill.kmp.library`; source sets in `core/database/CLAUDE.md`): SQLDelight (`JustChillDatabase`, the schema and migrations), mappers, and `SnapshotStore` over the six tables.
- `:core:backup` — KMP (`justchill.kmp.library`; the Ktor engine split in `core/backup/CLAUDE.md`), the snapshot file and the account it needs: DTOs, decoder, Supabase Storage, the backup cycle and auth. Never depends on `:core:database`.
- `:core:presentation` — the Compose-free half of the shared UI vocabulary (ADR 024 Decision 2), KMP `commonMain` (`justchill.kmp.library`, its tests in `androidHostTest`) on `:core:domain`, the JetBrains lifecycle-viewmodel, coroutines and datetime: the MVI base (`MviViewModel` and its contracts) in `mvi/`, `toUserMessage` in `error/`, the Spanish money, date and search formatters in `format/`, `CategoryUi` and `SelectableCategory` in `category/`, `TransactionUi` and `Catalog` in `transaction/`, `PersonBalanceUi` in `loan/`. No Compose.
- `:core:ui` — the Compose UI vocabulary more than one feature uses, on `:core:presentation`: the navigation vocabulary (`AppRoute`, `AppNavigator`, `NavHostBindings`) in `navigation/`, `AmountInputSheet` and the account/category/date pickers in `sheets/`, `FormSection` in `atoms/`, the design system (theme tokens, atoms, the `Emm*` components and the bundled fonts), and the render half of the capture vocabulary — the icon and colour catalog in `category/`, `TransactionRow` in `transaction/`. Never depends on `:core:database` or `:core:backup`.
- `:core:testing` — the test-fixture module, KMP: the fakes in `commonMain`, the JUnit4 `MainDispatcherRule` in `androidMain`; on `:core:domain` only, wired into feature modules and `:androidApp` as `testImplementation`, and into the KMP modules (`:core:domain`, `:core:presentation`) on `androidHostTest`. Fixture list: `core/testing/CLAUDE.md`.
- `:feature:{account, auth, category, loan, onboarding, profile, report, transaction}` — eight, one screen family each (ADR 015): Compose-free ViewModels, screens, `@Serializable` routes with a `<feature>Routes` registry, nav entries, a `<feature>Module` binding its ViewModels, tests.
- `:shared` — the umbrella KMP module (ADR 024 Decision 6) that exports the four core modules above and the seven KMP features as the `JustChillKit` framework, with SKIE; onboarding stays Android-only and outside it. `commonMain` holds the platform-neutral Koin modules (`kitModules`: `dataModule`, `backupModule`, `supabaseModule`, `sharedModule`, `commonCoreModule`), the backup orchestrator and the preference stores; `iosMain` the iOS actuals and `initKoin`. Details: `shared/CLAUDE.md`.
- `:androidApp` — `MainActivity`, `EmmApp`, the app shell (`shell/`: the nav host, its entry graph, the launcher shortcut routes, `rememberPlatformHostActions`), the Koin graph (`core/AppGraph.kt`) over `:shared`'s `kitModules` plus one `wiring/<Feature>Wiring.kt` per feature that binds something (onboarding injects nothing, so it has none), the platform Koin module, `startKoin`, the `dev` / `prod` flavors, shortcuts, the session keystore.

Shared Gradle configuration lives in convention plugins under `build-logic/convention` (`justchill.*`): `android.application`, `android.library`, `android.compose`, `android.feature`, `android.release`, `kmp.library` and `kmp.feature` (ADR 024: `android`, `iosArm64`, `iosSimulatorArm64`), `sqldelight`, detekt, `screenshot`, the quality gate, build info. They set the namespace from the module path, SDKs (`minSdk` 28), Java 17, opt-ins, test dependencies and each library's unit tests in the gate. A module build file applies its plugins and declares its own dependencies. `gradle/libs.versions.toml` is the only place a version is written; `kotlin-gradle-plugin`, and with it `:core:domain`'s stdlib, reaches build-logic at `kotlinVersion` through the `kotlin-compose` and `kotlin-serialization` markers (see the `kotlin-multiplatform` alias comment).

## Product

Manual capture of income and spending in soles, in under fifteen seconds per movement; accounts, categories, a month report, and informal loans as a parallel ledger. Spanish only, free, no ads, no bank sync. The Won't-have rows and the acceptance criterion: `docs/PRODUCT_REQUIREMENTS.md`.

## Non-negotiable rules

These bind on every change, including a new file created before any Kotlin has been read.

- **No comments.** No KDoc, no `//`, no banners, no commented-out code. The code explains itself or it gets renamed. Three narrow exceptions in `.claude/rules/kotlin-style.md`.
- **Explicit types** on every property and local `val` / `var`, and the supertype when the abstraction is what matters. Omit only when the right-hand side is a constructor call that already names the type.
- **Only the repo's atoms** (`:core:ui`'s `core/ui/atoms/`) in feature screens. Never a raw Material3 control. See `.claude/rules/ui-components.md`.
- **MVI per feature**: one `UiState` (all `val`), one `onIntent(intent)` entry point on `MviViewModel<S, I, E>`, effects consumed once and never stored in state. A ViewModel lives in its feature module and never imports Compose; `checkComposeFreeViewModels` is on the gate.
- **`:core:domain` stays pure Kotlin.** If it needs to reach outward, invert with an interface in `:core:domain`. Failure modes extend sealed `DomainException`, never a new exception type.
- **Dates take an injected `Clock` and `TimeZone`, no defaults.**
- **Rebuild, never adapt.** When existing code, config or structure does not fit the target architecture, replace it with a clean implementation. No shims, wrappers or compatibility patches over legacy.
- **`./gradlew qualityGate assembleDevDebug` green** before every commit. `qualityGate` is the gate; its task list is the `qualityGate` task `description`, never a prose copy.
- **Every route the nav host can push is `@Serializable`.** The crash is on process-death restore only, invisible to the compiler; `RouteSerializationTest` is the net.
- **A `CREATE TABLE` change ships its three artifacts**: the `.sq` edit, the `N.sqm`, the `databases/(N+1).db`, plus an instrumented test per starting version. See `.claude/rules/sqldelight.md`.
- **English for every identifier; Spanish only in user-facing values**, addressing the reader as tú, never vos.
- **Never add `Co-Authored-By`** from Claude, Anthropic or any AI assistant to a commit message; a hook blocks it. Conventional commits, linear history. Never push `trunk` without being asked, with one exception: the orchestrator pushes the docs-only commits that close a wave (the dispatch log) on its own, as Gema does.

## Detailed rules

Path-scoped, loaded when matching files are touched:

| File | Covers |
|---|---|
| `.claude/rules/architecture.md` | Layer boundaries, dependency inversion, use-case admission, errors, the MVI contract, Koin, routes |
| `.claude/rules/naming.md` | Uncle Bob, official Kotlin, naming patterns by layer, English identifiers |
| `.claude/rules/kotlin-style.md` | Explicit types, comment policy, Kotlin idioms, complexity limits, Compose sizing |
| `.claude/rules/principles.md` | YAGNI, KISS, SOLID with its tests, DRY with its caveat, what is rejected |
| `.claude/rules/ui-components.md` | The atoms iron rule, which token to reach for and why |
| `.claude/rules/sqldelight.md` | Schema changes: the three artifacts a migration ships, the migration test |
| `.claude/rules/github-workflows.md` | CI, the gate's definition, pinned actions, the release upload |

Each module carries a `CLAUDE.md` with its build and test facts and its feature gotchas.

## Stack

Kotlin, Jetpack Compose, Navigation 3, Koin, SQLDelight 2, supabase-kt with Ktor, Crashlytics on `prod` only. No Analytics.

## Commands

- `scripts/justchill-ci` — runs `CI=true ./gradlew qualityGate assembleDevDebug` on a clean, pushed HEAD and posts the `local-gate` status PRs require; `--dry-run` prints it instead.
- `./gradlew qualityGate` — the gate CI runs, defined in `QualityGateConventionPlugin.kt`; its `qualityGate` task `description` is the task list. `ConventionPluginTest`'s gate constants pin the per-module closure only.
- `./gradlew detekt` — the lint the gate runs on every module (ADR 018), one config in `config/detekt/detekt.yml` and no baseline. Locally it rewrites formatting in place; with `CI=true` it reports it instead, and that is how CI fails on it.
- `./gradlew assembleDevDebug` — dev debug build; `assembleProdRelease` for the release.
- `./gradlew :androidApp:validateDevDebugScreenshotTest` — the capture pad's screenshot matrix (`@PreviewWindowEdges`), on the gate; `updateDevDebugScreenshotTest --rerun` re-renders the references after an intended visual change. It lives in `:androidApp` because the screenshot plugin does not support KMP modules (ADR 024 Decision 5). Update never deletes a stale PNG: clear `androidApp/src/screenshotTestDevDebug/reference/` first when a cell is renamed or dropped.
- `./gradlew test` — every Android module's host tests; per module `:<module>:testDebugUnitTest`, and `:androidApp:testDevDebugUnitTest` for the MockK ViewModel suite. A KMP module's host tests run under `:<module>:testAndroidHostTest`, which `test` never reaches and the gate names; `rg -l justchill.kmp -g "{core,feature}/*/build.gradle.kts" -g "shared/build.gradle.kts"` lists those modules.
- `./gradlew :core:database:connectedAndroidDeviceTest` — the migration suite, on `justchill-api36`. `docs/agents/multi-session.md` `## Isolation: emulators` holds the pool split.
- Test tasks go `UP-TO-DATE` or `FROM-CACHE` across sessions: `--rerun-tasks` re-runs every task in the invocation, `--no-build-cache` stops a stale cache hit.

## Test stack

JUnit4, MockK, `kotlinx-coroutines-test`, plain `kotlin.test` where it suffices. Test names are backtick sentences naming the rule (`` `refuses while live dependents exist`() ``). Fixture locals are named by role. `MainDispatcherRule` (`:core:testing`) goes in every ViewModel test that touches `viewModelScope`; a ViewModel that injects `TodayFlow` takes `:core:testing`'s fake. A test that waits observes the transition, never samples the state: subscribe before triggering and assert the recorded sequence. A getter whose deletion leaves the suite green is not covered.

## Custom slash commands

- `/checks` — `./gradlew qualityGate assembleDevDebug`, failures grouped by module.
- `/feature <Name>` — full MVI scaffold for a new `:feature:<name>` module.
- `/agents-review` — review the pending diff against these rules.
- `/release` — tag trunk and upload a draft to the Play alpha track.
- `/wave <issues>` — boot one peer session per ticket and dispatch.

## Final rule

If a doc contradicts the current code, the code wins and the doc gets updated afterwards.

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues for `emm3000/Just-Chill` via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Design and reference docs

- `gh issue list --label ready-for-agent` — the committed work. No doc holds a work list.
- `core/database/CLAUDE.md` `## Snapshots` before touching `core/database/src/**/backup/`; `.claude/rules/sqldelight.md` before a `.sq`, a `.sqm` or a migration test.
- `docs/PRODUCT_REQUIREMENTS.md` — the Won't-have rows (ADRs amend them by row id), the NFRs, the acceptance criterion. Read before scoping a feature.
- `docs/play/` (advertising ID, listing, privacy policy) and `docs/release.md` — the store-facing set. Read before a Play submission, a privacy change or a release tag.

### Domain docs

Single-context: `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Multi-session orchestration

Playbook for the writer/reviewer loop and for dispatching to parallel peer sessions. Read it before dispatching any ticket. See `docs/agents/multi-session.md`.
