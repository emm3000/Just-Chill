# E03-05 — Classify a wrong-length IV as unreadable

**Epic:** [E03 — session secrets](../epics/E03-session-secrets.md)

## Done when

- [ ] a payload whose iv half is valid Base64 but the wrong byte count is either matched by
  `willNeverReadBack()`, or the decision to leave it in the KEEP path is written down here instead
- [ ] a host test pins whichever behaviour is chosen
- [ ] the fix does not widen `willNeverReadBack()` beyond exceptions this specific defect can throw

## Context

`SessionPayloadCodec.unwrap` validates Base64, never iv length. Only external corruption of the
stored value reaches this — `writeEncrypted` is the sole writer and writes one atomic full string.
It fails at `Cipher.init` in `KeystoreSessionCipher.decipher` as `InvalidAlgorithmParameterException`,
a type `willNeverReadBack()` does not match, so `reportUnreadableSession` keeps and re-warns forever.
