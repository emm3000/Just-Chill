# CLAUDE.md

Operating manifest for this repo. Loaded in every session.

## Mandatory state

The product is a **local-first** personal finance app. SQLDelight on device is the source of truth for reads and writes; the app is fully usable with no account and no network. Supabase exists for one thing, the opt-in **snapshot backup** (ADR 009): no row replication, no multi-device convergence. The sync engine is gone and is not coming back; its schema stays.

No third-party users, but **the author runs the release daily on a device holding real accumulated data**. UI is cheap to redo; a destructive migration is not.

## Modules

```
androidApp   -> ui-android, presentation, data, domain
ui-android   -> presentation, data, domain
presentation -> data, domain
data         -> domain
```

- `:domain` — **pure Kotlin** (`kotlin("jvm")`): models, value objects, use cases and the repository interfaces. `kotlinx-coroutines-core` and `kotlinx-datetime` only.
- `:data` — the domain interfaces implemented: SQLDelight (`EmmDatabaseData`, the schema and migrations), Supabase auth and backup, mappers.
- `:presentation` — the compose-free MVI core, every ViewModel with its `UiState` / `Intent` / `Effect`, the Koin modules, formatters.
- `:ui-android` — Compose screens, navigation, theme tokens and atoms. Same Kotlin packages as `:presentation` on purpose.
- `:androidApp` — `MainActivity`, `EmmApp`, the platform Koin module, the `dev` / `prod` flavors, shortcuts, the session keystore.

Shared Gradle configuration lives in convention plugins under `build-logic/convention` (`justchill.*`): `android.application`, `android.library`, `android.compose`, `android.feature`, `android.release`, `jvm.library`, `sqldelight`, detekt, the quality gate, build info. They set the namespace from the module path, SDKs (`minSdk` 28), Java 17, opt-ins, test dependencies and each library's unit tests in the gate. A module build file applies its plugins and declares its own dependencies. `gradle/libs.versions.toml` is the only place a version is written, with one exception: `:domain`'s stdlib comes from a pin in `build-logic/convention/build.gradle.kts`, and dropping it compiles `:domain` a minor version behind and reddens the gate on opt-in errors that name nothing about the classpath.

## Product

Manual capture of income and spending in soles, in under fifteen seconds per movement; accounts, categories, recurring movements, a month report, and informal loans as a parallel ledger. Spanish only, free, no ads, no bank sync. The Won't-have rows and the acceptance criterion: `docs/PRODUCT_REQUIREMENTS.md`.

## Non-negotiable rules

These bind on every change, including a new file created before any Kotlin has been read.

- **No comments.** No KDoc, no `//`, no banners, no commented-out code. The code explains itself or it gets renamed. Three narrow exceptions in `.claude/rules/kotlin-style.md`.
- **Explicit types** on every property and local `val` / `var`, and the supertype when the abstraction is what matters. Omit only when the right-hand side is a constructor call that already names the type.
- **Only the repo's atoms** (`ui-android/.../core/ui/atoms/`) in feature screens. Never a raw Material3 control. See `.claude/rules/ui-components.md`.
- **MVI per feature**: one `UiState` (all `val`), one `onIntent(intent)` entry point on `MviViewModel<S, I, E>`, effects consumed once and never stored in state. ViewModels live in `:presentation` and never import Compose.
- **`:domain` stays pure Kotlin.** If it needs to reach outward, invert with an interface in `:domain`. Failure modes extend sealed `DomainException`, never a new exception type.
- **Dates take an injected `Clock` and `TimeZone`, no defaults.**
- **Rebuild, never adapt.** When existing code, config or structure does not fit the target architecture, replace it with a clean implementation. No shims, wrappers or compatibility patches over legacy.
- **`./gradlew qualityGate assembleDevDebug` green** before every commit. `qualityGate` is the gate; plain `./gradlew detekt` covers strictly less and is never a substitute.
- **Every route the nav host can push is `@Serializable`.** The crash is on process-death restore only, invisible to the compiler; `RouteSerializationTest` is the net.
- **A `CREATE TABLE` change ships its three artifacts**: the `.sq` edit, the `N.sqm`, the `databases/(N+1).db`, plus an instrumented test per starting version. See `.claude/rules/sqldelight.md`.
- **English for every identifier; Spanish only in user-facing values**, addressing the reader as tú, never vos.
- **Never add `Co-Authored-By`** from Claude, Anthropic or any AI assistant to a commit message; a hook blocks it. Conventional commits, linear history, never push without being asked.

## Detailed rules

Path-scoped, loaded when matching files are touched:

| File | Covers |
|---|---|
| `.claude/rules/architecture.md` | Layer boundaries, dependency inversion, use-case admission, errors, the MVI contract, Koin, routes |
| `.claude/rules/naming.md` | Uncle Bob, official Kotlin, naming patterns by layer, English identifiers |
| `.claude/rules/kotlin-style.md` | Explicit types, comment policy, Kotlin idioms, detekt and its baselines, Compose sizing |
| `.claude/rules/principles.md` | YAGNI, KISS, SOLID with its tests, DRY with its caveat, what is rejected |
| `.claude/rules/ui-components.md` | The atoms iron rule, which token to reach for and why |
| `.claude/rules/sqldelight.md` | Schema changes: the three artifacts a migration ships, the migration test |
| `.claude/rules/github-workflows.md` | CI, the gate's definition, pinned actions, the release upload |

Each module carries a `CLAUDE.md` with its build and test facts and its feature gotchas.

## Stack

Kotlin, Jetpack Compose, Navigation 3, Koin, SQLDelight 2, supabase-kt with Ktor, Crashlytics on `prod` only. No Analytics.

## Commands

- `./gradlew qualityGate` — detekt per module, host tests, `:data`'s instrumented compile, `verifySqlDelightMigration`, `:androidApp:lintDevDebug`, `:build-logic:convention:test`. Defined once in `QualityGateConventionPlugin.kt`; the pre-push hook and CI run exactly it.
- `./gradlew assembleDevDebug` — dev debug build; `assembleProdRelease` for the release.
- `./gradlew test` — every module's host tests; per module `:<module>:testDebugUnitTest`, `:domain:test`, and `:androidApp:testDevDebugUnitTest` for the MockK ViewModel suite.
- `./gradlew :data:connectedDebugAndroidTest` — the migration suite, on the `medium_phone` emulator, the only AVD.
- Test tasks go `UP-TO-DATE` across sessions: `--rerun` forces a real run, per task.

## Test stack

JUnit4, MockK, `kotlinx-coroutines-test`, plain `kotlin.test` where it suffices. Test names are backtick sentences naming the rule (`` `refuses while live dependents exist`() ``). Fixture locals are named by role. `MainDispatcherRule` goes in every ViewModel test that touches `viewModelScope`; a ViewModel that injects `TodayFlow` takes a fake.

## Custom slash commands

- `/checks` — `./gradlew qualityGate assembleDevDebug`, failures grouped by module.
- `/feature <Name>` — full MVI scaffold across `:presentation` and `:ui-android`.
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
- `data/CLAUDE.md` `## Backup` before touching `data/src/**/backup/`; `.claude/rules/sqldelight.md` before a `.sq`, a `.sqm` or a migration test.
- `docs/PRODUCT_REQUIREMENTS.md` — the Won't-have rows (ADRs amend them by row id), the NFRs, the acceptance criterion. Read before scoping a feature.
- `docs/play/` (advertising ID, listing, privacy policy) and `docs/release.md` — the store-facing set. Read before a Play submission, a privacy change or a release tag.

### Domain docs

Single-context: `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Multi-session orchestration

Playbook for the writer/reviewer loop and for dispatching to parallel peer sessions. Read it before dispatching any ticket. See `docs/agents/multi-session.md`.
