# E03-04 — Decide how `KeystoreSessionCipher` gets test coverage

**Epic:** [E03 — session secrets](../epics/E03-session-secrets.md)

## Done when

- [ ] one option below (or another) is picked and the reasoning is written down
- [ ] the decision is acted on: either the new coverage exists, or the manual device check that
      substitutes for it is written down where a release checklist would find it

## Context

`KeystoreSessionCipher` never executes in any test: `AndroidKeyStore` is a stub in the JVM
host-test `android.jar`, and `:androidApp` has no instrumented source set — the repo's only one is
in `:data` (root `CLAUDE.md`). A wrong `TAG_SIZE_BITS` would ship on a green gate. Related:
`TAG_SIZE_BITS` is applied on decrypt only — encrypt relies on `AndroidKeyStore` defaulting to a
128-bit GCM tag. Fail-safe (a mismatch fails decryption, not weakens it), but untested and implicit.

Options to weigh, not to pick here:
- add an instrumented source set to `:androidApp` — new infra, and `qualityGate` doesn't run it
- accept the gap, write down the manual device check that substitutes for it
- pull the separator/Base64 parsing out from behind the Keystore call so at least that half is
  host-testable, leaving only the key operations uncovered
