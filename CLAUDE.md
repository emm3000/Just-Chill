# CLAUDE.md

Operating manifest for this repo. Loaded in every session.

## Mandatory state

The product is a **local-first** personal finance app. SQLDelight on device is the source of truth for reads and writes; the app is fully usable with no account and no network. Supabase exists for one thing, the opt-in **snapshot backup** (ADR 009): no row replication, no multi-device convergence. The sync engine is gone and is not coming back; its schema stays.

No third-party users, but **the author runs the release daily on a device holding real accumulated data**. UI is cheap to redo; a destructive migration is not.

## Modules

```
androidApp     -> feature:*, core:backup, core:database, core:ui, core:domain
feature:*      -> core:ui, core:domain, core:testing
core:backup    -> core:domain
core:database  -> core:domain
core:ui        -> core:domain
core:testing   -> core:domain
```

- `:core:domain` — **pure Kotlin** (`kotlin("jvm")`): models, value objects, use cases and the repository interfaces. `kotlinx-coroutines-core` and `kotlinx-datetime` only.
- `:core:database` — the domain interfaces implemented: SQLDelight (`JustChillDatabase`, the schema and migrations), mappers, and `SnapshotStore` over the six tables.
- `:core:backup` — the snapshot file and the account it needs: DTOs, decoder, Supabase Storage, the backup cycle and auth. Never depends on `:core:database`.
- `:core:ui` — the UI vocabulary more than one feature uses: the MVI base (`MviViewModel` and its contracts), the navigation vocabulary (`AppRoute`, `AppNavigator`, `NavHostBindings`) in `navigation/`, `toUserMessage` in `error/`, `AmountInputSheet` and the account/category/date pickers in `sheets/`, `FormSection` in `atoms/`, the Spanish money, date and search formatters, the design system (theme tokens, atoms, the `Emm*` components and the bundled fonts), and the capture vocabulary — the icon and colour catalog in `category/`, the transaction row and its `Catalog` in `transaction/`. Never depends on `:core:database` or `:core:backup`.
- `:core:testing` — the JVM test-fixture module, on `:core:domain` only, wired into feature modules, `:core:ui` and `:androidApp` as `testImplementation`. Fixture list: `core/testing/CLAUDE.md`.
- `:feature:{account, auth, category, loan, onboarding, profile, report, transaction}` — eight, one screen family each (ADR 015): Compose-free ViewModels, screens, `@Serializable` routes with a `<feature>Routes` registry, nav entries, a `<feature>Module` binding its ViewModels, tests.
- `:androidApp` — `MainActivity`, `EmmApp`, the app shell (`shell/`: the nav host, its entry graph, the launcher shortcut routes, `rememberPlatformHostActions`), the Koin graph (`core/AppGraph.kt`) over the cross-cutting modules in `core/di/` plus one `wiring/<Feature>Wiring.kt` per feature that binds something (onboarding injects nothing, so it has none), the backup orchestrator and the lifecycle and preference ports in `core/`, the platform Koin module, the `dev` / `prod` flavors, shortcuts, the session keystore.

Shared Gradle configuration lives in convention plugins under `build-logic/convention` (`justchill.*`): `android.application`, `android.library`, `android.compose`, `android.feature`, `android.release`, `jvm.library`, `sqldelight`, detekt, `screenshot`, the quality gate, build info. They set the namespace from the module path, SDKs (`minSdk` 28), Java 17, opt-ins, test dependencies and each library's unit tests in the gate. A module build file applies its plugins and declares its own dependencies. `gradle/libs.versions.toml` is the only place a version is written, with one exception: the `org.jetbrains.kotlin.jvm.gradle.plugin` marker in `build-logic/convention/build.gradle.kts` pins `:core:domain`'s resolved stdlib via `libs.versions.kotlinVersion` (see the `kotlin-jvm` alias comment), and dropping it compiles `:core:domain` a minor version behind and reddens the gate on opt-in errors that name nothing about the classpath.

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
- `./gradlew :feature:transaction:validateDebugScreenshotTest` — the capture pad's screenshot matrix (`@PreviewWindowEdges`), on the gate; `updateDebugScreenshotTest --rerun` re-renders the references after an intended visual change. Update never deletes a stale PNG: clear `src/screenshotTestDebug/reference/` first when a cell is renamed or dropped.
- `./gradlew test` — every module's host tests; per module `:<module>:testDebugUnitTest`, `:core:domain:test`, and `:androidApp:testDevDebugUnitTest` for the MockK ViewModel suite.
- `./gradlew :core:database:connectedDebugAndroidTest` — the migration suite, on `justchill-api36`. `docs/agents/multi-session.md` `## Isolation: emulators` holds the pool split.
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
- `core/database/CLAUDE.md` `## Backup` before touching `core/database/src/**/backup/`; `.claude/rules/sqldelight.md` before a `.sq`, a `.sqm` or a migration test.
- `docs/PRODUCT_REQUIREMENTS.md` — the Won't-have rows (ADRs amend them by row id), the NFRs, the acceptance criterion. Read before scoping a feature.
- `docs/play/` (advertising ID, listing, privacy policy) and `docs/release.md` — the store-facing set. Read before a Play submission, a privacy change or a release tag.

### Domain docs

Single-context: `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Multi-session orchestration

Playbook for the writer/reviewer loop and for dispatching to parallel peer sessions. Read it before dispatching any ticket. See `docs/agents/multi-session.md`.
