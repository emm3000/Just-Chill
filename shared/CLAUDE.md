# :shared — CLAUDE.md

The umbrella KMP module (ADR 024 Decision 6) on `justchill.kmp.library` plus SKIE. It exports the `JustChillKit` framework, and `:androidApp` depends on it. Root package `com.emm.justchill.core`, the one `:androidApp` used for these files before they moved, so its test suites over them stayed where they were.

## What it holds

- `JustChillKit` exports 11 modules: `:core:{domain, presentation, database, backup}` and the seven KMP features (`account`, `auth`, `category`, `loan`, `profile`, `report`, `transaction`). `:feature:onboarding` has no ViewModel and no Koin module, so it stays `com.android.library` and outside the framework. An exported module is an `api` dependency of `commonMain`, and the framework's `export(...)` list reads the same `exportedModules` list in `build.gradle.kts`.
- `commonMain`, `core/di/`: `kitModules` = `dataModule` (repositories, data sources, `SnapshotStore`, the backup ports), `backupModule` (the orchestrator, its metadata store, export history, disclosure signal), `supabaseModule` (the lazy `SupabaseClient`, `AuthRepository`, `GetSessionStatusUseCase`), `sharedModule` (`UniqueIdProvider`, `TodayFlow`, the only `Clock` and `TimeZone` in the graph) and `commonCoreModule` (`AppPreferences`, the app scope). A kit module never names a platform type; its platform inputs come from the app's platform module.
- `commonMain`, the rest: `BackupOrchestrator`, `DefaultBackupMetadataStore`, `LocalExportHistory`, `BackupDisclosureSignal`, `AppPreferences`, `SupabaseConfig`, `DefaultUniqueIdProvider`, `ioDispatcher`, and `backgroundEvents()` / `resumeEvents()` as `expect`.
- `androidMain`: the two lifecycle edges over `ProcessLifecycleOwner`.
- `iosMain`: the lifecycle edges over UIKit's app notifications, `iosPlatformModule`, `NSLogDiagnosticsLogger`, and `initKoin(KitConfig)`, the iOS entry. Preferences sit on `NSUserDefaultsSettings`; the session on supabase-kt's `SettingsSessionManager` over `KeychainSettings`.

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

`named("googleServerClientId")` is bound on both, but only `:feature:auth`'s wiring reads it. `CommitHash` stays Android-only: its one reader is `AppNavHost`. Swift reads its secrets from its own build configuration and passes them in `KitConfig`; nothing here hardcodes one. Until iOS has feature wiring, the iOS graph resolves no feature ViewModel.

## Backup

- No prod install holds a backup preference key yet (`prod` keeps backup off), so renaming one is free. Keys live in `DefaultBackupMetadataStore` over raw `Settings`. The prefixes, the `-1L` "never" value and the `'|'` separator are load-bearing. The streak count and its reason share one key (`count|REASON`) because `Settings` has no transaction. `LocalExportHistory`'s unscoped `last_local_export_at` is the one exception, and a saved export records itself only when the SAF write succeeded.
- The disclosure check comes first in `takeSnapshot`, before the due check a manual request skips; `uploader.upload` has that one call site. `DestinationUndisclosed` and `OwnerChanged` are refusals: no streak, no reason, no watermark; only a manual `OwnerChanged` emits `BackupEvent.Failed`. Failure state is booked against the captured account and published only while it is still signed in (`publishHealth`); the watermark is rechecked after upload.
- Stale means both more than `BACKUP_STALE_AFTER_DAYS` and a ledger that moved since the last verified snapshot.
- The `SupabaseClient` is a plain `single`, built on first resolution and never with `createdAtStart`; building it with no stored session sends no request, which keeps the app usable without an account (PRD §2 Offline).

## Build and test

- `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` links the framework to `build/bin/iosSimulatorArm64/debugFramework/JustChillKit.framework`; the gate runs it. No simulator is involved (ADR 024 Decision 8).
- `./gradlew :shared:testAndroidHostTest` runs `KitGraphKoinTest`. The suites over the moved classes stay in `:androidApp` (`src/test/.../core/`), which alone sees every module they wire.
- `skie { analytics { disableUpload } }` stays on: the app sends no telemetry, and neither does its build.
- The ObjC export warns about `description` clashes on every data class with that field; SKIE and the link still succeed.
