# E03 — Session secrets

## Why

The Supabase refresh token is the account. It is the one value in this app worth stealing, and iOS
still keeps it in the clear in `NSUserDefaults`.

## Constraints

- A persisted credential lives in the platform's secret store — Android Keystore, iOS Keychain — or
  it does not persist. `SharedPreferences` and `NSUserDefaults` are not secret stores. They are the
  easy path, which is why this epic exists.
- **The Android Keystore is reached through the platform APIs, never through
  `androidx.security:security-crypto`.** That library reached 1.1.0-stable and deprecated every one
  of its APIs in the same 2025 cycle — "in favour of existing platform APIs and direct use of
  Android Keystore" — so `EncryptedSharedPreferences` and `MasterKey` would be a dead dependency
  holding the one secret worth stealing.
- `SessionManager` is bound **once per platform**: `AndroidPlatformModule.kt` on Android,
  `KoinIos.kt` on iOS. A credential added later inherits whatever backing store that binding already
  has, so the binding is the thing to review — never the call site.
- **Android Auto Backup stays off.** `allowBackup="false"` exists precisely so the token cannot ride
  into Google's cloud, and encrypting the store does not make it safe to turn back on: a Keystore key
  is device-bound and does not survive a restore. The cost of that choice is tracked separately, in
  [E01-29](../backlog/E01-29-decide-what-a-lost-phone-costs.md).
- **On iOS, off-device backup is the default and has to be opted out of.** `NSUserDefaults` rides
  into the iCloud device backup; a Keychain item only stays out of it with an explicit
  device-only accessibility class.
- **Moving a credential off a cleartext store has to be eager.** `SessionManager` hangs off a lazy
  Koin `single`, and nothing on the Android startup path — or on the first screen — resolves the
  Supabase client: a migration that only runs inside `loadSession()` does not run at all until the
  user opens the account screen, so the cleartext survives indefinitely on an existing install.
  `EmmApp` sweeps at launch instead. The same trap waits on iOS.
- Encrypting the store is not the same as rotating what is in it. A token that leaked while it was in
  the clear stays valid after the move — closing this epic does not invalidate anything already read.

## Manual device check

Host tests stop at `SessionPayloadCodec` (E03-04): the `AndroidKeyStore` round trip, the 128-bit GCM
tag, and `generateSessionKey`'s corrupt-alias recovery only run on a device.

- **Round trip.** Plant a cleartext session by hand — `adb push` a crafted
  `shared_prefs/justchill_auth.xml`, `run-as <applicationId> cp` it into place — cold start, and
  confirm `user.email` surfaces in Perfil. Proves the sweep both encrypted and decrypted for real.
- **128-bit tag.** Sign in once, kill and relaunch: Perfil still shows the session. A wrong tag length
  fails decryption outright, so there is no separate partial-corruption state to probe.
- **Corrupt-alias recovery.** With a session already saved, wipe the device's Keystore-backed keys
  without touching app data (Settings → Security → Encryption & credentials → Clear credentials, or
  the OS-version equivalent) and relaunch. The app must land on the login screen, never crash at
  `onCreate`, and a fresh sign-in must persist normally afterward.
