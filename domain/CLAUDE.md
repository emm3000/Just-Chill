# :domain — CLAUDE.md

Plain `org.jetbrains.kotlin.jvm` library (ADR 011 — no Android or multiplatform plugin) and the
bottom of the graph — no module dependencies at all.

Only `kotlinx-coroutines-core` and `kotlinx-datetime` are allowed here. `android.*`/`androidx.*`
simply cannot resolve — with no Android artifact on this module's compile classpath, that is a
dependency-graph guarantee, not a convention to police. SQLDelight, Supabase and Ktor stay out by
convention: :domain declares repository interfaces, `:data` implements them.

Root package `com.emm.domain.<entity>`: one directory per entity under
`domain/src/main/kotlin/com/emm/domain/`, plus `shared/`. Read the directory instead of a list
written here. Naming is `[Verb][Noun]UseCase`, `{Entity}` with no suffix, and `{Entity}Repository`,
each in its entity's package.

## Error model

Sealed `DomainException` in `shared/error/` is the canonical failure type for every repository and
use case. Read the sealed class for the current subtypes — a list here goes stale silently and
already did. Don't leak SQLDelight or platform types into this module; those translations happen in
`:data`.

## Backend-agnostic interfaces

Repository interfaces declared here must not reference SQLDelight, Supabase, or any other framework.
`TransactionRepository` lives here; its SQLDelight-backed `DefaultTransactionRepository` lives in
`:data`.

The app is local-first; sync is **backup-only, one device at a time** (ADR 006). The row-replication
engine is gone (`docs/work/epics/E01-snapshot-backup.md`, ADR 009); auth ports live in `auth/`.
`sync/` now holds only `SyncMutex`, shared by `DeleteUserAccountUseCase` and `BackupOrchestrator` so
an account deletion and a backup upload never race each other. See ADRs 001 / 006 / 009.
