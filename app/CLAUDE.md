# :app — CLAUDE.md

Android application module. Compose UI, ViewModels, Koin DI wiring.

Root package: `com.emm.justchill.{hh.<feature>, core, components}`. `minSdk = 28`.

Depends on: `:domain`, `:data`.

## Product flavors

Dimension `tier`:
- `dev` — `applicationIdSuffix = ".dev"`.
- `prod` — release signing via `keystore.properties`, Firebase Analytics + Crashlytics.

## Layer conventions

| Concept | Naming | Location |
|---|---|---|
| ViewModel | `{Feature}ViewModel` | `hh/<feature>/` |
| UI state | `{Feature}UiState` | `hh/<feature>/` |

Existing features under `hh/`: `account`, `auth`, `category`, `home`, `onboarding`, `profile`, `recurring`, `report`, `seetransactions`, `shared`, `transaction`, plus `di/`.

## Data flow

`Screen` collects `StateFlow<UiState>` from `ViewModel` → `ViewModel` calls a domain use case → use case calls a `Repository` interface (impl lives in `:data`).

ViewModels should **never** depend on SQLDelight types directly — go through domain interfaces.

## DI (Koin)

Modules live in `hh/di/` (plus `core/CoreModule.kt`):

- `dbModule` — SQLDelight driver + `EmmDatabaseData`
- `supabaseModule` — `SupabaseClient` (auth + postgrest)
- `accountModule`, `categoryModule`, `transactionModule`, `authModule` — feature wiring (data sources, repository impls, use cases, ViewModels)
- `syncModule` — per-table `TableSync` units (qualified), `DefaultSyncRepository`, `SyncDataUseCase` (**single** — holds the mutex that serializes sync cycles), `SyncCursorStore` adapter
- `hhModule` — top-level aggregator

A new feature module should be registered in `EmmApp` alongside the others.

## UI

- Jetpack Compose + Material3 (Compose BOM in `libs.versions.toml`).
- **Navigation3** (`androidx.navigation3:navigation3-runtime` + `-ui` + `androidx.lifecycle:lifecycle-viewmodel-navigation3`) using a type-safe `NavBackStack`. Navigation Compose 2.x is also on the classpath but **new screens should use Navigation3**.
- Coil 2.x (`io.coil-kt:coil-compose`) for image loading.
- `material-icons-extended` is available.

## Error handling

`core/error/DomainExceptionExt.kt` maps each `DomainException` subtype to a user-facing **Spanish** string via `DomainException.toUserMessage()`. ViewModels should surface errors through this extension, not raw exception messages.

## Testing

- `MainDispatcherRule` at `app/src/test/kotlin/com/emm/justchill/MainDispatcherRule.kt` — **use it in every ViewModel test that touches `viewModelScope`**.
- JUnit4 + MockK + `kotlinx-coroutines-test`.
- Instrumented tests in `app/src/androidTest/` are for behaviour that depends on the Android runtime (Compose UI tests, etc.).
