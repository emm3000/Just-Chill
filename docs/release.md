# Release Checklist

This is the ordered gate a JustChill release passes, from the last commit on `trunk` to a build a
person actually promotes in Play Console. Nothing on this page is optional.

## Before the tag

- **Gate and build.** Run `/release`'s own pre-flight, `./gradlew qualityGate lintProdRelease`,
  before tagging. `qualityGate` has exactly one definition, in `build-logic` — don't hand-write a
  substitute task list here.
- **QA passes**, on a real device:
  - clean install
  - a week of offline-first use
  - late sign-in — use the app accountless first, sign in after
  - sign-out
  - a real upgrade over an old APK with `adb install -r` — never a fresh install; only an upgrade
    runs the migrations against real data
- **Session device check.** Host tests stop at `SessionPayloadCodec`; the `AndroidKeyStore` round
  trip and the 128-bit GCM tag only run on a device (`androidApp/CLAUDE.md` `## Session`).
  - Round trip: plant a cleartext session under `LEGACY_SESSION_KEY` by hand (`adb push` a crafted
    `shared_prefs/justchill_auth.xml`, `run-as <applicationId> cp` it into place), cold start, and
    confirm `user.email` surfaces in Perfil. That proves the sweep encrypted into
    `ENCRYPTED_SESSION_KEY` and decrypted for real.
  - Tag: sign in once, kill and relaunch; Perfil still shows the session. A wrong tag length fails
    decryption outright, there is no partial-corruption state to probe.
  - Discard vs. keep: flip one byte in the ciphertext half of a stored `ENCRYPTED_SESSION_KEY`
    value, cold start; the app lands on the login screen and logs "discarded and signed out"
    (`AEADBadTagException` through `willNeverReadBack()` into `reportUnreadableSession`'s
    `prefs.edit { remove(...) }`). An untouched value keeps the session.
  - Named gap: `generateSessionKey`'s delete-and-regenerate path fires only when the Keystore alias
    is present but unreadable, and nothing short of instrumented Keystore corruption reaches it.
- **Restore drill.** [ADR 009](adr/009-backup-is-a-snapshot-not-row-replication.md) Decision 4
  requires a continuously proven restore before backup ships, and this line is its only
  enforcement: delete it and nothing in the repo asks for the drill again.
  - While `SNAPSHOT_BACKUP_ENABLED` is `false` (its state today), the drill runs over the manual
    export: export from the installed build, record Cuentas' `Saldo total` and the Cuentas and
    Categorías counts first, import onto a clean install, and confirm all three figures and the
    ledger survive.
  - Once the flag flips, the drill runs over the newest verified snapshot instead.
  - Read the figures from the app, not from the import snackbar: `buildImportDoneMessage` never
    reports accounts or categories, and omits any clause whose count is zero.
- **The declarations agree.** The advertising-ID answer, the Data Safety form,
  [`play/privacy-policy.md`](play/privacy-policy.md) and [`play/listing.md`](play/listing.md)
  describe one app; changing one without the rest produces two official statements that contradict
  each other. Re-verify the advertising ID with [`play/advertising-id.md`](play/advertising-id.md)
  rather than re-deriving it.
- **If this release flips `SNAPSHOT_BACKUP_ENABLED`**, update the privacy policy, the store listing
  and the Data Safety answer in this same release — the app stops being "nothing leaves your phone".
  Not after. Flipping the flag is a compliance event, not a feature flag; a "Yes" set to unblock
  one release becomes permanent.

## The tag

[`/release`](../.claude/commands/release.md) does the tagging and the pre-flight. Nothing else here.

## After the workflow

The release is judged by the `Publish to Play Store` step, not by `Upload AAB artifact`. A green run
reached no one; a red run may still have uploaded the artifact and failed only at the Edit commit.
A tag that looks shipped may have shipped nothing.

- The workflow leaves a **draft** on the alpha track. Publishing it is manual: Play Console → Pruebas
  → Alfa, confirm the draft's `versionCode`/`versionName`, publish.
- Play validates declarations against the binary **and against every release still active in a
  track** — an old active release can fail an otherwise correct declaration.
