# E03-02 — Move the iOS session into the Keychain

**Epic:** [E03 — session secrets](../epics/E03-session-secrets.md)

## Done when

- [ ] the session no longer lives in `NSUserDefaults`
- [ ] the Keychain item carries a device-only accessibility class, so it stays out of the iCloud
  backup
- [ ] the binding change is covered by whatever proves the iOS graph still builds

## Context

`KoinIos.kt` binds `SessionManager` to `SettingsSessionManager(NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults))`.
The file's own comment already says the Keychain is where this belongs. This needs a
`SessionManager` implementation of its own — `NSUserDefaultsSettings` has no Keychain equivalent to
swap in.
