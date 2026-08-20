# E03-03 — Report the undecryptable-session case to diagnostics

**Epic:** [E03 — session secrets](../epics/E03-session-secrets.md)

## Done when

- [ ] `KeystoreSessionManager.loadSession()` distinguishes "a session was stored and failed to
      decrypt" from "nothing was stored"
- [ ] the undecryptable case reaches `DiagnosticsLogger.warn`
- [ ] the fresh-install case (nothing stored) stays silent — no `DiagnosticsLogger` call, no
      Crashlytics noise

## Context

`KeystoreSessionCipher.decrypt` returns `null` when the Keystore key is lost or invalidated;
`readEncryptedSession()` maps that straight to `null`, and `loadSession()` throws
`NoSessionFoundException` either way. supabase-kt's `AuthImpl.loadFromStorage` catches that and logs
"No session found in storage" at debug level — indistinguishable from a fresh install. The one
failure mode this design most needs to observe on a real device is the one it currently hides.
