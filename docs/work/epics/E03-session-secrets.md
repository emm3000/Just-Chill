# E03 — Session secrets

## Why

The Supabase refresh token is the account. It is the one value in this app worth stealing, and today
both platforms keep it in the clear — Android in `shared_prefs/justchill_auth.xml`, iOS in
`NSUserDefaults`.

## Constraints

- A persisted credential lives in the platform's secret store — Android Keystore, iOS Keychain — or
  it does not persist. `SharedPreferences` and `NSUserDefaults` are not secret stores. They are the
  easy path, which is why this epic exists.
- `SettingsSessionManager` is bound **once per platform**: `AndroidPlatformModule.kt` on Android,
  `KoinIos.kt` on iOS. A credential added later inherits whatever backing store that binding already
  has, so the binding is the thing to review — never the call site.
- **Android Auto Backup stays off.** `allowBackup="false"` exists precisely so the token cannot ride
  into Google's cloud, and encrypting the store does not make it safe to turn back on: a Keystore key
  is device-bound and does not survive a restore. The cost of that choice is tracked separately, in
  [E01-29](../backlog/E01-29-decide-what-a-lost-phone-costs.md).
- **On iOS, off-device backup is the default and has to be opted out of.** `NSUserDefaults` rides
  into the iCloud device backup; a Keychain item only stays out of it with an explicit
  device-only accessibility class.
- Encrypting the store is not the same as rotating what is in it. A token that leaked while it was in
  the clear stays valid after the move — closing this epic does not invalidate anything already read.
