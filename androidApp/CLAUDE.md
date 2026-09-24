# :androidApp — CLAUDE.md

Android entry point, app shell and composition root: `MainActivity`, `EmmApp`, the Navigation 3 host, its entry graph and the SAF host actions in `shell/`, the Koin graph in `core/AppGraph.kt` over the cross-cutting modules in `core/di/` plus one wiring file per feature in `wiring/`, the backup orchestrator and metadata store in `core/backup/`, the process-lifecycle edges in `core/lifecycle/`, `AppPreferences` in `core/preferences/`, the platform halves of the ports the features declare (the platform Koin module, the Crashlytics `DiagnosticsLogger` sink, `CurrentActivityHolder`, the Google sign-in launcher), and the `@Preview` host in `components/`. Every screen, ViewModel and route belongs to a `:feature:*`.

`com.android.application` on AGP 9's built-in Kotlin, root package `com.emm.justchill`, `minSdk = 28`, `compileSdk = 37`. Depends on the nine `:feature:*` modules, on `:core:ui` and `:core:domain`, and on `:core:database` and `:core:backup`, which only this module sees: `core/di/DataModule.kt` binds every repository and `SnapshotStore`, and `AndroidPlatformModule` constructs the SQLDelight driver itself.

## The shell

- `shell/AppNavHost.kt` owns the back stack, the root `Scaffold` with its `SnackbarHostState`, the SAF launchers and the result channels, which it hands to `shell/AppEntryGraph.kt` as one `HostResultChannels`; that file holds the nine features' `*Entries` calls and every cross-feature route the host supplies. `rememberPlatformHostActions` (`shell/PlatformHostActions.kt`) builds the export and import SAF launchers and must be called at that root, never inside an `entry<...> { }` body, or a picker result arriving after its entry left composition is dropped. `AppNavigator` and `NavHostBindings` are `:core:ui`'s; `RouteSerializationTest` concatenates one route registry per feature.
- `HOME_ROUTE` (`shell/AppEntryGraph.kt`) is the amount pad, `AddTransactionRoute()` with no preselection, and it is the only thing that sits at index 0 (ADR 017): the app opens on it, the manifesto's `replaceAll` lands on it, and the system back gesture exits the app from it the way a home screen does. No permanent strip of chrome survives: the pad's corner icon pushes `ProfileRoute`, the "Más" menu, and every menu row pushes above that.
- `shell/ShortcutRoutes.kt` is the launcher intent contract: `MainActivity` flattens an `Intent` into `ShortcutIntent`, `ShortcutPublisher` writes the same action and extra keys, and `ShortcutXmlActionsTest` pins them against both `shortcuts.xml` copies.
- `wiring/<Feature>Wiring.kt` is one file per feature that binds something, listed in `appModules()`, binding that feature's use cases and `includes(<feature>Module)`. Eight features have one; onboarding injects nothing, so it has none. What no single feature owns is a module in `core/di/` instead: `dataModule` (repositories, data sources, `SnapshotStore`, the backup ports), `backupModule` (the orchestrator, its metadata store and export history), `supabaseModule` (the `SupabaseClient`) and `sharedModule` (`UniqueIdProvider`, `TodayFlow`, and the only `Clock` and `TimeZone` in the graph).

## Platform Koin module

`androidPlatformModule` (`core/AndroidPlatformModule.kt`) supplies only genuine platform bits, including the `GoogleSignInLauncher` implementation (`core/auth/ActivityGoogleSignInLauncher.kt`) bound against `:feature:auth`'s interface. `startKoin` lives here and nowhere else: it needs `androidContext()` / `androidLogger()` from koin-android. A feature's own bindings go in its `wiring/` file, never here. `AndroidPlatformModuleTest` covers most of these bindings one test at a time, but not the sign-in launcher; `AppGraphKoinTest` builds the graph against `TestPlatformModule` and so cannot see any of them.

## Product flavors

Dimension `tier`. `dev` adds `applicationIdSuffix = ".dev"`. `justchill.android.release` owns the version from git, R8 and the `release` signing config from `keystore.properties`; `prod` opts into that config. Crashlytics is declared for every variant, but `src/dev/AndroidManifest.xml` sets `firebase_crashlytics_collection_enabled=false`, which keeps the privacy policy's "the dev flavor is telemetry-free" claim true. Firebase Analytics is not used.

## Session

- The Supabase refresh token is the account. It persists only in the Android Keystore through platform APIs (`core/session/`); `androidx.security:security-crypto` is deprecated and stays out. `SharedPreferences` is not a secret store.
- `SessionManager` is bound once, in `AndroidPlatformModule.kt`. A credential added later inherits that backing store, so the binding is what gets reviewed, never the call site.
- `allowBackup="false"` stays. A Keystore key is device-bound and does not survive a restore, so encrypting the store does not make Auto Backup safe.
- The cleartext sweep is eager and never throws: `EmmApp` runs `sweepLegacySession` at launch under `runCatching`, because `SessionManager` is a lazy Koin `single` nothing on the startup path resolves. Moving a credential does not rotate it.
- A malformed payload is rejected in `SessionPayloadCodec`, never left to the cipher. Only `IllegalArgumentException` matches `willNeverReadBack()`; `Cipher.init` throws `InvalidAlgorithmParameterException`, which lands in the KEEP path and re-warns forever from a site no host test reaches.
- Host tests stop at `SessionPayloadCodec`. The Keystore round trip is the manual device check in `docs/release.md`.

## Spend shortcuts

- The amount digits are irreducible: every entry point shortens the path to the amount pad and never adds a field, chip row or sheet to `AddTransactionScreen`.
- Which combos to surface is core logic (`GetFrequentCombosUseCase`, `:core:domain`); pushing them to the launcher is Android (`ShortcutManagerCompat`, `TileService`, Glance stay here).
- A preselection from outside the app always arrives on a cold start, before the account and category catalogs land. An id-based selection has no expiry, so it survives late data (a host test pins the ordering) and is consumed once per ViewModel (`preselectConsumed`): the entry's `LaunchedEffect(key)` restarts on rotation, theme change and pop-back, and a second firing silently reverts the user's choice.
- A combo can name a deleted account or category: resolve by id, fall back to the normal defaults, never crash, never show an empty selection.
- Flavor resources replace, never merge: `src/main/res/xml/shortcuts.xml` and `src/dev/res/xml/shortcuts.xml` are two full copies, every change lands in both, and `ShortcutXmlActionsTest` pins the action strings against both.
- Shortcut slots are one budget, manifest plus dynamic, with a platform floor of five. Prod: one static (`loans`) plus three combos; dev: two statics, exactly at the floor. A third static makes `setDynamicShortcuts` throw, the throw is swallowed, and combos quietly never appear.
- A shortcut navigates with `AppNavigator.pushToTop`, never `push`. `MainActivity` consumes the launch intent only when `savedInstanceState == null`.
- `TileService#startActivityAndCollapse(Intent)` throws on target 34+; use `setActivityLaunchForClick(pendingIntent)`. A tile is adopted only through `StatusBarManager.requestAddTileService` (API 33+), version-guarded; below 33, do not offer it.
- Rejected: a `RemoteInput` notification and a notification listener reading Yape/BCP (a Play-policy minefield for a local-only app).

## Backup

The cycle lives in `core/backup/`; the file format and the account are `:core:backup`, the rows `:core:database`, the Perfil rows `:feature:profile`.

- `BackupAvailability` (`:core:domain`) is `FlavorBackupAvailability`, bound in `core/di/BackupModule.kt` over the `SNAPSHOT_BACKUP_ENABLED` `buildConfigField` each flavor declares in `build.gradle.kts`: `dev` true, `prod` false. Readers here, both skipping when it is false: `bootstrapAppGraph`, which skips `start()` and leaves `BackupOrchestrator` bound and lazily resolvable, and `BackupDisclosureSignal`, whose `isPending` gates to `flowOf(false)` because with the orchestrator stopped `canUploadToDestination` stays false for every account and an ungated signal would announce a gate that cannot run.
- Flipping `prod` is a disclosure change first: `docs/play/privacy-policy.md`, `docs/play/listing.md`, the in-app `PrivacyPolicyScreen` and the Play Data Safety form change in the same release. It is also blocked by the verify phrase: `EmmSnackbar` draws two lines and `toPhrase()` appends table clauses in order, so the last tables' counts get cut off, and ADR 009 makes those counts part of the ship gate.
- Until then no install holds a backup preference key, so renaming one is free. Keys live in `DefaultBackupMetadataStore` over raw `Settings`. The prefixes, the `-1L` "never" value and the `'|'` separator are load-bearing. The streak count and its reason share one key (`count|REASON`) because `Settings` has no transaction. `LocalExportHistory`'s unscoped `last_local_export_at` is the one exception, and a saved export records itself only when the SAF write succeeded.
- The disclosure check comes first in `takeSnapshot`, before the due check a manual request skips; `uploader.upload` has that one call site. `DestinationUndisclosed` and `OwnerChanged` are refusals: no streak, no reason, no watermark; only a manual `OwnerChanged` emits `BackupEvent.Failed`. Failure state is booked against the captured account and published only while it is still signed in (`publishHealth`); the watermark is rechecked after upload.
- Stale means both more than `BACKUP_STALE_AFTER_DAYS` and a ledger that moved since the last verified snapshot.

## Testing

`./gradlew :androidApp:testDevDebugUnitTest`; no instrumented source set. A feature's ViewModel suite lives with its ViewModel; what stays here is what needs a module a feature may not see. `AppGraphKoinTest`, `RouteSerializationTest` and `ShortcutRoutesTest` live here because the shell does; `RouteSerializationTest` concatenates each module's route registry and round-trips the union. `ProfileViewModelBackupFailureTest` and `TransactionDateEndToEndTest` sit under their feature's package here, because they wire a real `BackupOrchestrator` and a real repository over in-memory SQLite. `MainDispatcherRule` (`:core:testing`) goes in every ViewModel test that touches `viewModelScope`.

`AppGraphKoinTest` resolves the whole graph off-device against `TestPlatformModule`; a missing binding compiles clean, so it is the only net before a launch. It cannot see a definition nothing in the graph resolves: `EXPECTED_VIEW_MODELS` and `every single bootstrapAppGraph resolves is bound` plug the two ways in, and `CommitHash`, whose sole consumer is a `koinInject` in `AppNavHost`, is covered by `AndroidPlatformModuleTest` instead. With a sentinel `Clock` and `TimeZone` bound, it also asserts by identity that every `Clock` / `TimeZone` field on a `com.emm.` class is the bound instance.

The snapshot tests that run a JSON file into real SQLite live here too (`src/test/.../core/backup/`), for the same reason: only the app sees `:core:backup`, which writes the file, and `:core:database`, which owns the rows. `src/test/resources/backup/snapshot-v4-trunk.json` is what the exporter produced before the split, and `GoldenSnapshotRestoreTest` fails the day a format change stops reading it.
