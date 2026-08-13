# :domain — CLAUDE.md

Kotlin Multiplatform library. Targets: `android` (host tests only) + `iosArm64` + `iosSimulatorArm64`.
**No Android, JVM, or framework dependencies.** No module dependencies — this is the bottom of the graph.

Only `kotlinx-coroutines-core` and `kotlinx-datetime` are allowed here. Anything from `java.*`,
`android.*`, SQLDelight, Supabase or Ktor breaks the iOS compile — that break is the guardrail
working as intended, not an obstacle to route around.

Root package: `com.emm.domain.<entity>`. Source in `domain/src/commonMain/kotlin/`.
Packages: `account`, `auth`, `category`, `home`, `recurring`, `report`, `sync`, `transaction`,
`shared/` (`error/`, `backup/`).

## Layer conventions

| Concept | Naming | Location |
|---|---|---|
| Use case | `[Verb][Noun]UseCase` (e.g. `CreateTransactionUseCase`, `DeleteCategoryUseCase`) | `<entity>/` |
| Entity / value | `{Entity}` (no suffix) | `<entity>/` |
| Repository interface | `{Entity}Repository` | `<entity>/` |

## Error model

Sealed `DomainException` in `shared/error/` is the canonical failure type for all repositories and
use cases. Subtypes: `NotFound`, `ValidationError`, `DatabaseError`, `Unauthorized`,
`NetworkUnavailable`, `Unknown`.

When adding a new failure mode, extend `DomainException` instead of introducing a new exception
type. Don't leak SQLDelight or platform types into this module — those translations happen in `:data`.

## Backend-agnostic interfaces

Repository interfaces declared here must not reference SQLDelight, Supabase, or any other
framework. Example: `TransactionRepository` lives here; its `DefaultTransactionRepository`
(SQLDelight-backed) lives in `:data`.

The app is local-first; sync is **backup-only, one device at a time** (ADR 006). The row-replication
engine below is scheduled for deletion (`docs/sync/ADR009_PLAN.md` Phase 5): auth ports live in
`auth/` (`AuthRepository`, `ObserveSessionUseCase`, `SessionStatus`, claim use cases) and sync ports
in `sync/` (`SyncRepository`, `SyncCursorStore`, `SyncDataUseCase`, `ConflictResolver` — pure LWW).
Supabase implementations live in `:data`; the `SyncCursorStore` adapter lives in `:presentation`
(`core/sync/DefaultSyncCursorStore.kt`). See `docs/adr/001` / `docs/adr/006` / `docs/adr/009`.

## Testing

- Primary unit-test surface for the whole project — fast, no device.
- Tests live in `domain/src/androidHostTest/kotlin/` (JUnit4 + MockK). A `commonTest` source set is
  declared in `build.gradle.kts` but has no files yet; MockK is only on the host-test classpath, so
  anything that mocks belongs in `androidHostTest` regardless.
- Use `runTest`, `mockk()`, `coEvery`, `coVerify`.
- Run: `./gradlew :domain:testAndroidHostTest`
- Single test: `./gradlew :domain:testAndroidHostTest --tests "com.emm.domain.transaction.CreateTransactionUseCaseTest"`
- The task goes `UP-TO-DATE` across sessions — add `--rerun` to force a real run.
- There is no `:domain:test` task. It disappeared when the module became KMP.
