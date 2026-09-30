# :shared — CLAUDE.md

The umbrella KMP module (ADR 024 Decision 6) on `justchill.kmp.library` plus SKIE. It exports the `JustChillKit` framework, and `:androidApp` depends on it. Root package `com.emm.justchill.core`, the one `:androidApp` used for these files before they moved, so its test suites over them stayed where they were.

## What it holds

- `JustChillKit` exports 11 modules: `:core:{domain, presentation, database, backup}` and the seven KMP features (`account`, `auth`, `category`, `loan`, `profile`, `report`, `transaction`). `:feature:onboarding` has no ViewModel and no Koin module, so it stays `com.android.library` and outside the framework. An exported module is an `api` dependency of `commonMain`, and the framework's `export(...)` list reads the same `exportedModules` list in `build.gradle.kts`.
- `commonMain`, `core/di/`: `kitModules` = `dataModule` (repositories, data sources, `SnapshotStore`, the backup ports), `backupModule` (the orchestrator, its metadata store, export history, disclosure signal), `supabaseModule` (the lazy `SupabaseClient`, `AuthRepository`, `GetSessionStatusUseCase`), `sharedModule` (`UniqueIdProvider`, `TodayFlow`, the only `Clock` and `TimeZone` in the graph) and `commonCoreModule` (`AppPreferences`, the app scope). A kit module never names a platform type; its platform inputs come from the app's platform module.
- `commonMain`, `wiring/` (package `com.emm.justchill.wiring`): one `<Feature>Wiring.kt` per feature that binds something, binding its use cases and `includes(<feature>Module)`, collected in `featureWirings`. Both apps start the same list: `:androidApp`'s `appModules()` and `initKoin` are `kitModules + featureWirings + <platform module>`, so a use case is bound once for both platforms.
- `commonMain`, the rest: `BackupOrchestrator`, `DefaultBackupMetadataStore`, `LocalExportHistory`, `BackupDisclosureSignal`, `AppPreferences`, `SupabaseConfig`, `DefaultUniqueIdProvider`, `ioDispatcher`, and `backgroundEvents()` / `resumeEvents()` as `expect`.
- `androidMain`: the two lifecycle edges over `ProcessLifecycleOwner`.
- `iosMain`: the lifecycle edges over UIKit's app notifications, `iosPlatformModule`, `NSLogDiagnosticsLogger`, `NoCredentialsGoogleSignInLauncher`, and `initKoin(KitConfig)`, the iOS entry. Preferences sit on `NSUserDefaultsSettings`; the session on supabase-kt's `SettingsSessionManager` over `KeychainSettings`.
- `iosMain`, `core/viewmodel/`: Swift cannot call Koin's reified `get()`, so every ViewModel has one top-level `resolve<Name>Handle` function in its feature's `<Feature>ViewModels.kt` (`resolveAccountsHandle()`, `resolveEditTransactionHandle(transactionId:)`, ...; navigation parameters as `String`, `AddCategoryViewModel`'s type as the `CategoryType` name), and `AppPreferences` has `resolveAppPreferences()` in `core/preferences/`. A new ViewModel gets an accessor and a line in `ViewModelAccessorsTest`.

## What a platform module supplies

Every binding below must come from `androidPlatformModule` or `iosPlatformModule`; `KitGraphKoinTest` resolves `kitModules` against a test platform module holding exactly these.

| Binding | Android | iOS |
|---|---|---|
| `SqlDriver`, `JustChillDatabase` | `provideSqlDriver(context)` | `provideSqlDriver()` |
| `Settings` | `SharedPreferencesSettings` over `justchill_prefs` | `NSUserDefaultsSettings` |
| `SessionManager` | `KeystoreSessionManager` | `SettingsSessionManager(KeychainSettings)` |
| `DiagnosticsLogger` | `CrashReportingDiagnosticsLogger` | `NSLogDiagnosticsLogger` |
| `named("appVersion")` | `BuildConfig.VERSION_NAME` | `KitConfig.appVersion` |
| `SupabaseConfig` | `BuildConfig` via `withOfflineFallback` | `KitConfig` via `withOfflineFallback` |
| `BackupAvailability` | `FlavorBackupAvailability` | `KitConfig.isSnapshotBackupEnabled` |

`named("googleServerClientId")` and `GoogleSignInLauncher` are bound on both, but only `:feature:auth` reads them; iOS answers `GoogleSignInResult.NoCredentials` until #541 brings the Google SDK. `CommitHash` stays Android-only: its one reader is `AppNavHost`. Swift reads its secrets from its own build configuration and passes them in `KitConfig`; nothing here hardcodes one.

## The ViewModel handle

- An accessor returns an `MviHandle<S, I, E>`, whose ViewModel stays `internal` so Swift drives it only through the handle: `currentState`, `send(intent)`, `collectState` / `collectEffects` taking a closure, and `clear()`. The handle builds its ViewModel through `ViewModelProvider.create` over a `ViewModelStore` it owns; `clear()` clears that store, which cancels `viewModelScope`. SwiftUI never calls `onCleared()`, so the Swift owner calls `clear()` when it goes away (`iosApp/CLAUDE.md`, the store).
- SKIE bridges neither `MviViewModel.state` nor `effect`: both are typed by the class's own type parameters, so the header keeps a raw `id<Kotlinx_coroutines_coreFlow>`, and no concrete ViewModel redeclares them. The handle collects both in Kotlin on `viewModelScope` (`Dispatchers.Main.immediate`) and calls the Swift closure on the main thread; the same `clear()` stops them.
- Sealed `UiIntent` / `UiEffect` / `UiState` reach Swift as protocols, so a handle reads `MviHandle<AccountsUiState, any AccountsIntent, any AccountsEffect>`.

## Backup

- `startSnapshotBackup(koin)` (`commonMain`, `core/backup/SnapshotBackupStart.kt`) is the one start for both apps: it calls `BackupOrchestrator.start()` only when `BackupAvailability.isAvailable`. `:androidApp`'s `bootstrapAppGraph` calls it, and `initKoin` calls it right after `startKoin`; `@HiddenFromObjC` keeps it off Swift's header.
- `BackupDisclosureWatch` (`commonMain`) collects `BackupDisclosureSignal.isPending` in Kotlin and hands each value to a Swift closure, the way `MviHandle.collectState` does. `iosMain`'s `resolveBackupDisclosureWatch()` (`core/backup/BackupDisclosureWatchAccessor.kt`) builds one on `MainScope()` for `AppShell`'s Más badge. `stop()` cancels that scope, so a stopped watch never starts again; resolve a new one.
- No prod install holds a backup preference key yet (`prod` keeps backup off), so renaming one is free. Keys live in `DefaultBackupMetadataStore` over raw `Settings`. The prefixes, the `-1L` "never" value and the `'|'` separator are load-bearing. The streak count and its reason share one key (`count|REASON`) because `Settings` has no transaction. `LocalExportHistory`'s unscoped `last_local_export_at` is the one exception, and a saved export records itself only when the SAF write succeeded.
- The disclosure check comes first in `takeSnapshot`, before the due check a manual request skips; `uploader.upload` has that one call site. `DestinationUndisclosed` and `OwnerChanged` are refusals: no streak, no reason, no watermark; only a manual `OwnerChanged` emits `BackupEvent.Failed`. Failure state is booked against the captured account and published only while it is still signed in (`publishHealth`); the watermark is rechecked after upload.
- Stale means both more than `BACKUP_STALE_AFTER_DAYS` and a ledger that moved since the last verified snapshot.
- The `SupabaseClient` is a plain `single`, built on first resolution and never with `createdAtStart`; building it with no stored session sends no request, which keeps the app usable without an account (PRD §2 Offline).

## Build and test

- `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` links the framework to `build/bin/iosSimulatorArm64/debugFramework/JustChillKit.framework`; the gate runs it. No simulator is involved (ADR 024 Decision 8).
- `./gradlew :shared:iosSimulatorArm64Test` runs `ViewModelAccessorsTest` on the booted `justchill-ios27` against the real `iosPlatformModule`: every accessor resolves and `clear()` cancels the scope. It overrides the `SqlDriver` with `provideSqlDriver(name)` on a per-run database and the `Settings` with a per-run `NSUserDefaults` suite, then deletes the database files and removes the suite domain: every test executable is `test.kexe`, so `standardUserDefaults` is one `test.kexe` domain shared by every run. It is not on the gate. Test executables link `-lsqlite3` (`build.gradle.kts`), or SQLiter's symbols stay undefined.
- `./gradlew :shared:testAndroidHostTest` runs `KitGraphKoinTest`. The suites over the moved classes stay in `:androidApp` (`src/test/.../core/`), which alone sees every module they wire.
- `skie { analytics { disableUpload } }` stays on: the app sends no telemetry, and neither does its build.
- The ObjC export warns about `description` clashes on every data class with that field; SKIE and the link still succeed.
