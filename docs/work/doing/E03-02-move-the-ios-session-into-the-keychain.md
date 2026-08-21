# E03-02 — Move the iOS session into the Keychain

**Epic:** [E03 — session secrets](../epics/E03-session-secrets.md)

## Done when

- [ ] the session no longer lives in `NSUserDefaults`
- [ ] the Keychain item carries a device-only accessibility class, so it stays out of the iCloud
  backup
- [ ] an existing install's cleartext session is swept into the Keychain and removed from
  `NSUserDefaults` at launch, before anything resolves the Supabase client
- [ ] a host test pins the sweep, and something pins that launch still calls it
- [ ] the binding change is covered by whatever proves the iOS graph still builds

## Context

`KoinIos.kt` binds `SessionManager` to `SettingsSessionManager(NSUserDefaultsSettings(...))`.
`multiplatform-settings` already ships `KeychainSettings`, so the store swaps without a new
`SessionManager` — but only its vararg constructor sets `kSecAttrAccessible`; the `service` one
leaves the item at `kSecAttrAccessibleWhenUnlocked`, which rides into the backup. `initKoin()`
migrates nothing today.
