# :androidApp — CLAUDE.md

Thin Android entry point: `MainActivity`, `EmmApp`, and the Android halves of ports `:presentation`
declares — the platform Koin module, `DispatchersProvider`, the Crashlytics `DiagnosticsLogger` sink,
`CurrentActivityHolder`, the Google sign-in launcher, plus the `@Preview` host in `components/`.
Anything else belongs in another module: ViewModels, the MVI core and the Koin graph are
`:presentation`, Compose UI is `:ui-android`.

`com.android.application` on AGP 9's built-in Kotlin support. Root package `com.emm.justchill`,
`minSdk = 28`, `compileSdk = 37`. Depends on `:ui-android`, and on `:domain` and `:data` directly,
because `AndroidPlatformModule` constructs the SQLDelight driver itself.

## Platform Koin module

`androidPlatformModule` (`core/AndroidPlatformModule.kt`) supplies only genuine platform bits — read
the file. `startKoin` cannot be shared: it needs `androidContext()`/`androidLogger()` from
koin-android. A new feature module is registered in `appModules()` in `:presentation`, never here.
`AndroidPlatformModuleTest` covers these bindings; `AppGraphKoinTest` in `:presentation` cannot see
them.

## Product flavors

Dimension `tier`. `dev` adds `applicationIdSuffix = ".dev"` and carries
`src/dev/kotlin/.../experiences/`, a personal Compose playground that is not part of the product,
shipping its own `experiencesModule` (`prod` has an empty stub of the same file). `prod` gets release
signing from `keystore.properties`. Crashlytics is declared for every variant, but
`src/dev/AndroidManifest.xml` sets `firebase_crashlytics_collection_enabled=false`, which keeps the
privacy policy's "build the dev flavor for a telemetry-free app" claim true. Firebase Analytics is
not used.

## Session

- The Supabase refresh token is the account. It persists only in the Android Keystore, reached
  through platform APIs (`core/session/`); `androidx.security:security-crypto` deprecated every API
  it has and stays out. `SharedPreferences` is not a secret store.
- `SessionManager` is bound once, in `AndroidPlatformModule.kt`. A credential added later inherits
  that backing store, so the binding is what gets reviewed, never the call site.
- `allowBackup="false"` stays. A Keystore key is device-bound and does not survive a restore, so
  encrypting the store does not make Auto Backup safe to turn on.
- The cleartext sweep is eager and never throws: `EmmApp` runs `sweepLegacySession` at launch under
  `runCatching`, because `SessionManager` is a lazy Koin `single` nothing on the startup path
  resolves, so a migration inside `loadSession()` would never run on an existing install. Moving a
  credential does not rotate it: a token that leaked in the clear stays valid.
- A malformed payload is rejected in `SessionPayloadCodec`, never left to the cipher. Only
  `IllegalArgumentException` matches `willNeverReadBack()`; `Cipher.init` throws
  `InvalidAlgorithmParameterException`, which lands in the KEEP path and re-warns forever from a
  site no host test reaches.
- Host tests stop at `SessionPayloadCodec`. The Keystore round trip and the GCM tag are the manual
  device check in `docs/RELEASE_CHECKLIST.md`.

## Spend shortcuts

- The amount digits are irreducible: every entry point shortens the path to the amount pad and
  never adds a field, chip row or sheet to `AddTransactionScreen`.
- Which combos to surface is core logic (`GetFrequentCombosUseCase`, `:domain`); pushing them to the
  launcher is Android (`ShortcutManagerCompat`, `TileService`, Glance stay here or in `:ui-android`).
- `selectFrequentCombo` no-ops until the account and category catalogs land, and a preselection from
  outside the app always arrives on a cold start, before them. It must survive late data (a host
  test pins the ordering) and is consumed once per ViewModel (`preselectConsumed`): the entry's
  `LaunchedEffect(key)` restarts on rotation, theme change and pop-back from `CategoryRoute`, and a
  second firing silently reverts the user's choice. Any future starting-state entry inherits this.
- A combo can name a deleted account or category: resolve by id, fall back to the normal defaults,
  never crash, never show an empty selection.
- Flavor resources replace, never merge: `src/main/res/xml/shortcuts.xml` and
  `src/dev/res/xml/shortcuts.xml` are two full copies, every change lands in both, and
  `ShortcutXmlActionsTest` pins the action strings against both.
- Shortcut slots are one budget, manifest plus dynamic, with a platform floor of five
  (`getMaxShortcutCountPerActivity()`). Prod: one static (`loans`) plus three combos; dev: two
  statics (`loans`, `random`), exactly at the floor. A third static makes `setDynamicShortcuts`
  throw, the throw is swallowed, and combos quietly never appear. Count both flavors first.
- A shortcut navigates with `AppNavigator.pushToTop`, never `push` (`.claude/rules/navigation.md`).
- `MainActivity` consumes the launch intent only when `savedInstanceState == null`: rotation and
  process-death restore re-deliver it.
- `TileService#startActivityAndCollapse(Intent)` throws on target 34+; use
  `setActivityLaunchForClick(pendingIntent)`. A tile only gets adopted through
  `StatusBarManager.requestAddTileService` (API 33+), version-guarded under `minSdk = 28`; below
  33, do not offer it.
- Rejected: a `RemoteInput` notification (free text needs a parser that fails silently, plus a
  permanent notification and `POST_NOTIFICATIONS`); a notification listener reading Yape/BCP (every
  notification on the phone, a Play-policy minefield for a local-only app).

## Testing

`./gradlew :androidApp:testDevDebugUnitTest`; no instrumented source set. `MainDispatcherRule` goes
in every ViewModel test that touches `viewModelScope`. The MockK ViewModel tests live here, not in
`:presentation`, although the ViewModels are in `presentation/src/main`: same package, MockK's JVM
engine. Keep that placement unless you move the whole suite.
