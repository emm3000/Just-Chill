# E04 — Release and compliance

## Why

`v2.4.0` is why this needs a home. It was tagged, built and uploaded — and Play refused to commit the
Edit because the bundle declared `AD_ID` while the Console declaration said it did not. The tag looks
shipped. Nothing shipped.

## Constraints

- **A green workflow is not a release, and a red one is not nothing.** `uploadRelease.yml` uploads to
  the alpha track as a draft that a human publishes in Play Console. A green run reached no one; a
  red run may still have uploaded the artifact and failed only at the Edit commit. Read the Edit
  step, never the upload step.
- **Play checks declarations against the binary, and against every release still active in a track**
  — not only against the bundle being uploaded. An old active release can be what fails an otherwise
  correct declaration.
- **The declarations move together.** The advertising-ID answer, the Data Safety form,
  `docs/PRIVACY_POLICY.md` and `docs/PLAY_STORE_LISTING.md` all describe one app. Changing one and
  not the rest produces two official statements that contradict each other, which is its own policy
  problem — and it is how a "Yes" set to unblock one release becomes permanent.
- **Flipping `SNAPSHOT_BACKUP_ENABLED` is a compliance event, not a feature flag.** The release that
  flips it updates the privacy policy, the store listing and the Data Safety answer in the same
  commit, because the app stops being "nothing leaves your phone".
- **The restore drill is required by
  [ADR 009](../../adr/009-backup-is-a-snapshot-not-row-replication.md) Decision 4 and is enforced
  only by a line on the release checklist.** Until that checklist exists as a file, the rule is
  prose.
