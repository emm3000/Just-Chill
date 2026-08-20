# E01-29 — Decide what a lost phone costs

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] the choice is made and recorded: either the app tells the user that nothing leaves the phone
      and a lost phone is a total loss, or the accountless mode stops being supported
- [ ] `docs/PRIVACY_POLICY.md` and the Play Data Safety answer agree with whichever was chosen
- [ ] the decision lands in the epic as a constraint; this ticket dies with `git rm`

## Context

`allowBackup="false"` in `androidApp/src/main/AndroidManifest.xml` is deliberate — it keeps the
Supabase refresh token out of Android Auto Backup. But `SNAPSHOT_BACKUP_ENABLED` is `false`, so the
cloud pipeline is dark for everyone, not only for users without an account. Today every lost phone
is a total loss and nothing in the app says so.
