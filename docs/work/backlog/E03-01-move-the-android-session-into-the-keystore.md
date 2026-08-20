# E03-01 — Move the Android session into the Keystore

**Epic:** [E03 — session secrets](../epics/E03-session-secrets.md)

## Done when

- [ ] the Supabase session no longer lands in plaintext `shared_prefs/justchill_auth.xml`
- [ ] the backing store is keyed by the Android Keystore, and a dependency that provides it is
  actually declared (nothing in `gradle/libs.versions.toml` provides one today)
- [ ] an existing install migrates without forcing a re-login, or the re-login is a deliberate,
  stated choice

## Context

`AndroidPlatformModule.kt` binds `SessionManager` to `SettingsSessionManager(SharedPreferencesSettings(...))`
over `AUTH_PREFS_NAME`, with `Context.MODE_PRIVATE` and no encryption layer. A repo-wide search
finds no `androidx.security:security-crypto`, `EncryptedSharedPreferences` or `MasterKey`.
