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
- **Manual device check.** Run the checks in
  [E03 § Manual device check](work/epics/E03-session-secrets.md#manual-device-check).
- **Restore drill.** [ADR 009](adr/009-backup-is-a-snapshot-not-row-replication.md) Decision 4
  requires a continuously proven restore before backup ships, enforced by this drill.
  - While `SNAPSHOT_BACKUP_ENABLED` is `false` (its state today), the drill runs over the manual
    export: export from the installed build, record Reporte's current-month ingresos and gastos
    totals (Mes tab) and the Cuentas and Categorías counts first, import onto a clean install, and
    confirm all four figures and the ledger survive.
  - Once the flag flips, the drill runs over the newest verified snapshot instead.
  - Read the figures from the app, not from the import snackbar: `buildImportDoneMessage` never
    reports accounts or categories, and omits any clause whose count is zero.
- **The declarations agree.** The advertising-ID answer, the Data Safety form,
  [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) and [`PLAY_STORE_LISTING.md`](PLAY_STORE_LISTING.md)
  describe one app; changing one without the rest produces two official statements that contradict
  each other. Re-verify the advertising ID with [`PLAY_ADVERTISING_ID.md`](PLAY_ADVERTISING_ID.md)
  rather than re-deriving it.
- **If this release flips `SNAPSHOT_BACKUP_ENABLED`**, update the privacy policy, the store listing
  and the Data Safety answer in this same release — the app stops being "nothing leaves your phone".
  Not after.

## The tag

[`/release`](../.claude/commands/release.md) does the tagging and the pre-flight. Nothing else here.

## After the workflow

The release is judged by the `Publish to Play Store` step, not by `Upload AAB artifact`. A green run
reached no one; a red run may still have uploaded the artifact and failed only at the Edit commit.

- The workflow leaves a **draft** on the alpha track. Publishing it is manual: Play Console → Pruebas
  → Alfa, confirm the draft's `versionCode`/`versionName`, publish.
- Play validates declarations against the binary **and against every release still active in a
  track** — an old active release can fail an otherwise correct declaration.
