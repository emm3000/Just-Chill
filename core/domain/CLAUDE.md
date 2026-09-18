# :core:domain — CLAUDE.md

Plain `org.jetbrains.kotlin.jvm` library (ADR 011) and the bottom of the graph: no module dependencies at all. Only `kotlinx-coroutines-core` and `kotlinx-datetime`. `android.*` / `androidx.*` cannot resolve here, a dependency-graph guarantee; SQLDelight, Supabase and Ktor stay out by convention, reviewed.

Root package `com.emm.justchill.core.domain.<entity>`: one directory per entity plus `shared/` and `time/`. Read the directory instead of a list. Naming and the use-case admission rule: `.claude/rules/naming.md`, `.claude/rules/architecture.md`. The vocabulary: `CONTEXT.md` at the repo root.

`time/` holds `TodayFlow` and `ClockTodayFlow`, the one way a ViewModel derives the date; its `delay` loop schedules against `SystemClock.uptimeMillis()`, so `distinctUntilChanged` swallows a late post-doze wake.

## Error model

Sealed `DomainException` in `shared/error/` is the canonical failure type for every repository and use case. Read the sealed class for the current subtypes; a list here goes stale silently and already did.

## Deletion integrity

Deletes are soft, and foreign keys fire only on a physical `DELETE`, so each entity answers integrity here or in its data source:

- `DeleteAccountUseCase` refuses while live dependents exist.
- `DeleteCategoryUseCase` holds no rule, on purpose.
- `DeleteLoanUseCase` is a passthrough; the cascade soft-delete of the loan's payments is `LoanLocalDataSource.softDelete`'s transaction, and `LoanRepository.delete` owes it.

## Backup and session

The app is local-first; backup is a snapshot, **one device at a time** (ADR 006, ADR 009). The ports live in `shared/backup/`, the auth ports in `auth/`. `shared/RemoteWriteMutex.kt` is shared by `DeleteUserAccountUseCase` and `BackupOrchestrator` so an account deletion and a backup upload never race each other. It bounds only the acquisition (30s, then `DomainException.Busy`), never the work: `delete_account` is irreversible once it returns. `DeleteUserAccountUseCase.resolveAuthenticatedUserId` resolves the session once; `DefaultAuthRepository.deleteAccount` must not wait again inside the lock. `hasLocalChangesSince` is the single predicate for "is there anything to back up": the cycle gate and the UI warning must not be able to disagree.

## Testing

`./gradlew :core:domain:test`. Use cases are the primary test surface; a behavior test for every rule, fixture locals named by role.
