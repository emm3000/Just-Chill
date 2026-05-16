# :domain — CLAUDE.md

Pure Kotlin JVM library (`java-library` + `kotlin.jvm`). **No Android dependencies.** No module dependencies.

Root package: `com.emm.domain.<entity>`

## Layer conventions

| Concept | Naming | Location |
|---|---|---|
| Use case | `[Verb][Noun]UseCase` (e.g. `CreateTransactionUseCase`, `DeleteCategoryUseCase`) | `:domain/<entity>/` |
| Entity / value | `{Entity}` (no suffix) | `:domain/<entity>/` |
| Repository interface | `{Entity}Repository` | `:domain/<entity>/` |

## Error model

Sealed `DomainException` in `shared/error/` is the canonical failure type for all repositories and use cases. Subtypes: `NotFound`, `ValidationError`, `NetworkUnavailable`, `DatabaseError`, `Unauthorized`, `Unknown`.

When adding a new failure mode, extend `DomainException` instead of introducing a new exception type. Don't leak Android, Supabase, or SQLDelight types into this module — those translations happen in `:data`.

## Backend-agnostic interfaces

Repository interfaces declared here must not reference Supabase/Ktor/SQLDelight. Examples: `AuthRepository` lives here; its `DefaultAuthRepository` (Supabase-backed) lives in `:data`.

## Testing

- Primary unit-test surface for the whole project — fast, no Android.
- JUnit4 + MockK + `kotlinx-coroutines-test`.
- Use `runTest`, `mockk()`, `coEvery`, `coVerify`.
- Run: `./gradlew :domain:test`
- Single test: `./gradlew :domain:test --tests "com.emm.domain.transaction.CreateTransactionUseCaseTest"`
