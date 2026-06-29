# :androidApp — CLAUDE.md

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

Since the slice H dedup, the feature DI modules **and** the `:data`-binding wiring
(`supabaseModule`, `syncModule`, `authModule`, `dataModule`, `commonCoreModule`)
live in `shared-ui/commonMain` (`com.emm.justchill.hh.di` / `core`), not in
`:androidApp`. The shared module list and the post-`startKoin` bootstrap are exposed
via `appModules(platformModule)` + `bootstrapAppGraph(koin)` in
`shared-ui/commonMain/core/AppGraph.kt`.

Only genuine platform bits live in `androidPlatformModule`
(`androidApp/core/AndroidPlatformModule.kt`):

- DB `single` (driver + onCreate seed) + `EmmDatabaseData`
- `Settings` backend (`SharedPreferencesSettings`)
- `SupabaseConfig`
- `GoogleSignInLauncher` (+ `GoogleCredentialClient`)
- `appVersion` / `googleServerClientId` — named `String`s from `BuildConfig`
- `DispatchersProvider`
- `CurrentActivityHolder`

A new feature module is registered in `appModules()` (commonMain), **not** in `EmmApp`.

## UI

- Jetpack Compose + Material3 (Compose BOM in `libs.versions.toml`).
- **Navigation3** (`androidx.navigation3:navigation3-runtime` + `-ui` + `androidx.lifecycle:lifecycle-viewmodel-navigation3`) using a type-safe `NavBackStack`. Navigation Compose 2.x is also on the classpath but **new screens should use Navigation3**.
- Coil 2.x (`io.coil-kt:coil-compose`) for image loading.
- `material-icons-extended` is available.

## Error handling

`core/error/DomainExceptionExt.kt` maps each `DomainException` subtype to a user-facing **Spanish** string via `DomainException.toUserMessage()`. ViewModels should surface errors through this extension, not raw exception messages.

## Testing

- `MainDispatcherRule` at `androidApp/src/test/kotlin/com/emm/justchill/MainDispatcherRule.kt` — **use it in every ViewModel test that touches `viewModelScope`**.
- JUnit4 + MockK + `kotlinx-coroutines-test`.
- Instrumented tests in `androidApp/src/androidTest/` are for behaviour that depends on the Android runtime (Compose UI tests, etc.).
