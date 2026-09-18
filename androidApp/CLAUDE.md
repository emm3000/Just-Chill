# :androidApp — CLAUDE.md

Android entry point and app shell: `MainActivity`, `EmmApp`, the Navigation 3 host and bottom bar in `shell/`, the Koin graph in `core/AppGraph.kt` with one wiring file per feature in `wiring/`, and the Android halves of the ports `:presentation` declares (the platform Koin module, `DispatchersProvider`, the Crashlytics `DiagnosticsLogger` sink, `CurrentActivityHolder`, the Google sign-in launcher), plus the `@Preview` host in `components/`. ViewModels and the MVI core are `:presentation`; feature screens and their nav entries are `:ui-android`.

`com.android.application` on AGP 9's built-in Kotlin, root package `com.emm.justchill`, `minSdk = 28`, `compileSdk = 37`. Depends on the nine `:feature:*` modules and on `:ui-android`, and on `:core:domain` and `:core:database` directly because `AndroidPlatformModule` constructs the SQLDelight driver itself.

## The shell

- `shell/AppNavHost.kt` owns the back stack, the root `Scaffold` with its `SnackbarHostState`, the SAF launchers and the result channels (`pendingCategory`, `pendingImportJson`); it calls each feature's `*Entries` function, from `:feature:{account, category, loan, report}` for the extracted features and from `:ui-android`'s `hh/<feature>/` for the rest. `AppNavigator` and `NavHostBindings` are `:core:ui`'s; `RouteSerializationTest` concatenates one route registry per feature.
- `shell/AppBottomBar.kt` holds four tabs plus the centre add button, no more. Its 10sp labels already sit under the 4.5:1 AA floor; a fifth tab shrinks them further. A new destination swaps a tab out, never appends one.
- `shell/ShortcutRoutes.kt` is the launcher intent contract: `MainActivity` flattens an `Intent` into `ShortcutIntent`, `ShortcutPublisher` writes the same action and extra keys, and `ShortcutXmlActionsTest` pins them against both `shortcuts.xml` copies.
- `wiring/<Feature>Wiring.kt` is one file per feature, listed in `appModules()`, binding that feature's use cases and `includes(<feature>Module)`. `account`, `category`, `loan` and `report` are filled; the rest stay an empty Koin module until their extraction ticket fills it, and an extraction ticket edits its own file only.

## Platform Koin module

`androidPlatformModule` (`core/AndroidPlatformModule.kt`) supplies only genuine platform bits. `startKoin` lives here and nowhere else: it needs `androidContext()` / `androidLogger()` from koin-android. A feature's own bindings go in its `wiring/` file or, until it is extracted, in `:presentation`'s `hh/di/`, never here. `AndroidPlatformModuleTest` covers these bindings; `AppGraphKoinTest` builds the graph against `TestPlatformModule` and so cannot see them.

## Product flavors

Dimension `tier`. `dev` adds `applicationIdSuffix = ".dev"` and carries `src/dev/kotlin/.../experiences/`, a personal Compose playground outside the product, shipping its own `experiencesModule` (`prod` has an empty stub of the same file). `justchill.android.release` owns the version from git, R8 and the `release` signing config from `keystore.properties`; `prod` opts into that config. Crashlytics is declared for every variant, but `src/dev/AndroidManifest.xml` sets `firebase_crashlytics_collection_enabled=false`, which keeps the privacy policy's "the dev flavor is telemetry-free" claim true. Firebase Analytics is not used.

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
- `selectFrequentCombo` no-ops until the account and category catalogs land, and a preselection from outside the app always arrives on a cold start, before them. It survives late data (a host test pins the ordering) and is consumed once per ViewModel (`preselectConsumed`): the entry's `LaunchedEffect(key)` restarts on rotation, theme change and pop-back, and a second firing silently reverts the user's choice.
- A combo can name a deleted account or category: resolve by id, fall back to the normal defaults, never crash, never show an empty selection.
- Flavor resources replace, never merge: `src/main/res/xml/shortcuts.xml` and `src/dev/res/xml/shortcuts.xml` are two full copies, every change lands in both, and `ShortcutXmlActionsTest` pins the action strings against both.
- Shortcut slots are one budget, manifest plus dynamic, with a platform floor of five. Prod: one static (`loans`) plus three combos; dev: two statics, exactly at the floor. A third static makes `setDynamicShortcuts` throw, the throw is swallowed, and combos quietly never appear.
- A shortcut navigates with `AppNavigator.pushToTop`, never `push`. `MainActivity` consumes the launch intent only when `savedInstanceState == null`.
- `TileService#startActivityAndCollapse(Intent)` throws on target 34+; use `setActivityLaunchForClick(pendingIntent)`. A tile is adopted only through `StatusBarManager.requestAddTileService` (API 33+), version-guarded; below 33, do not offer it.
- Rejected: a `RemoteInput` notification and a notification listener reading Yape/BCP (a Play-policy minefield for a local-only app).

## Testing

`./gradlew :androidApp:testDevDebugUnitTest`; no instrumented source set. The MockK ViewModel tests live here, not in `:presentation`, although the ViewModels are in `presentation/src/main`: same package, MockK's JVM engine. Keep that placement unless you move the whole suite. `AppGraphKoinTest`, `RouteSerializationTest` and `ShortcutRoutesTest` live here because the shell does; `RouteSerializationTest` concatenates each module's route registry and round-trips the union. `MainDispatcherRule` (`:core:testing`) goes in every ViewModel test that touches `viewModelScope`.

The snapshot tests that run a JSON file into real SQLite live here too (`src/test/.../core/backup/`), for the same reason: only the app sees `:core:backup`, which writes the file, and `:core:database`, which owns the rows. `src/test/resources/backup/snapshot-v4-trunk.json` is what the exporter produced before the split, and `GoldenSnapshotRestoreTest` fails the day a format change stops reading it.
