# E01 — Snapshot backup

**Decision:** [ADR 009](../../adr/009-backup-is-a-snapshot-not-row-replication.md)
**Flag:** `SNAPSHOT_BACKUP_ENABLED` — flips when every E01 ticket closes

## Why

Replaces the row-replication sync engine with periodic full-JSON snapshot backup to Supabase
Storage: one blob per snapshot, staggered retention, one restore path (`importFromJson`). Phases
0–3 (ADR, sign-out fix, export v3, pipeline, health visibility) are closed; restore confidence
remains.

## Constraints

- No DB schema migration anywhere in this track; the export FORMAT version (v2→v3) is not the DB schema version.
- Never drop `userId` / `syncState` / `deletedAt`; never change UUID PKs; never revert to hard deletes — the sync schema stays so a future engine can plug in without re-migrating. Dead columns stay dead in place.
- Local rows are no longer stamped with a `userId` at all — `ClaimLocalDataOnAuthenticationUseCase` was the only writer of it and is gone; benign today (no read filters by `userId`, `DefaultBackupUploader` scopes by storage prefix) but a future engine plugging into the preserved sync schema needs a catch-up claim pass.
- That catch-up pass already exists as twelve SQL queries — `claimAll:`, `unclaimAll:` and `countUnclaimed:`, three in each of `accounts.sq`, `categories.sq`, `recurring_movements.sq` and `transactions.sq` — and since E01-28 deleted the claim machinery they have no Kotlin caller at all. They are kept on purpose: a dead-code sweep that removes them takes the future engine's re-entry point with them.
- No silent failure in the backup pipeline — every failure path in the uploader/pruner/verifier logs a distinct, named reason, and a new path adds a new reason instead of reusing one. To recount, list every `storageCall`, `wholeBucket` and explicit throw site in `data/.../backup/DefaultBackup{Uploader,Pruner,Verifier}.kt`; each `wholeBucket` forwards two reasons (list failed, listing never ended). No number lives here because the total moves with the producer boundary you draw.
- Backup preference keys live in `DefaultBackupMetadataStore` (`:presentation/core/backup/`) over raw `Settings`, not `AppPreferences`; the key prefixes, the `-1L` "never" sentinel and the `'|'` failure separator are byte-identical to pre-move and load-bearing.
- The failure streak's count and reason share **one** key (`count|REASON`) — `Settings` has no transaction, so two keys risk a count carrying the wrong reason across a kill.
- `SNAPSHOT_BACKUP_ENABLED` has never been `true`, so no install carries any backup preference key yet — renaming one is free today and stops being free the moment the flag flips.
- Flipping `SNAPSHOT_BACKUP_ENABLED` to `true` is a disclosure change before it is a release: `docs/PRIVACY_POLICY.md`, `docs/PLAY_STORE_LISTING.md` and the Play Data Safety declaration are updated in the same release that flips it, never after — all three currently state that no financial data leaves the device.
- Accountless mode stays supported (E01-29): today a lost or replaced phone without a prior export is a total loss, and Perfil says so — `BACKUP_LOCAL_ONLY_WARNING` (`:presentation`) renders under the backup `ProfileGroup` gated on `!SNAPSHOT_BACKUP_ENABLED`, so it disappears the same release cloud backup turns on.
- An undisclosed destination and `SnapshotOutcome.OwnerChanged` are refusals, not failures: no streak increment, no `BackupFailureReason`, no watermark touched, no `BackupEvent`. The uploader's pre-upload account assertion is the one exception — it throws and counts.
- No backup failure signs the user out; a failure reaches the UI only as `ProfileEffect.Notify`, never `ShowError`.
- Failure state is booked against the account the cycle captured and published only while that account is still signed in (`publishHealth` re-checks with `compareAndSet`); the watermark is re-checked after upload for the same reason.
- The disclosure gate is the first check inside `takeSnapshot`, ahead of the `isBackupDue` check a manual request skips — `uploader.upload` has exactly one call site, reachable only through that chain.
- `BackupRowUi` rank order is load-bearing: `NeedsAccount` > `DisclosurePending` > `BackingUp`.
- `DisclosurePending` severity is `Warning`, never `Danger` — the remedy is in the same row and fires on every first sign-in.
- A failure annotates the snapshot, never replaces it (`Failed` carries a `LastSnapshot`, never a nullable `Int`); warn on the failure count, never on the reason (`fromNameOrNull` can be null).
- Backup row state resolution and copy live in `:presentation` (`BackupRowUi.severity()`, `toMetaText()`), never in a composable — that is the `:presentation`/`:ui-android` split, and only a JVM test can pin it (`ProfileViewModelBackupRowTest` in `:androidApp` does).
- `TooManyFunctions` allows 11 per class; `BackupOrchestrator` and `ProfileViewModel` each sit at exactly 11 — the next member on either needs a top-level hoist first. `DefaultBackupRepository` is at 9.
- `:ui-android` has no Compose UI test harness, so neither `ProfileScreen` flag gate is asserted. `SNAPSHOT_BACKUP_ENABLED` has one home (`BackupKillSwitch.kt`) and four readers — `bootstrapAppGraph`, two in `ProfileScreen` (one negated) and `BackupDisclosureSignal`; the first and the last carry an explaining comment, and only the last is pinned by a test (`BackupDisclosureSignalTest`).
- The Perfil-tab badge and `BackupRowUi.DisclosurePending` are the same question asked twice — `disclosureIsPending` (`:presentation/core/backup/`, over `SessionStatus`, kill-switch gated) and `resolveBackupRow`'s ranked branch (over `SessionUiState`, gated by `ProfileScreen` instead). They must not be able to disagree.
- `AppGraphKoinTest` cannot see a binding that was never registered — anything reached only via direct `koinInject`/`koin.get` stays invisible; register every new binding or cover it with its own module test.
- Version gates are frozen literals (`BACKUP_RECURRING_SINCE_VERSION`, `BACKUP_SCHEMA_VERSION_V2`), never the live `BACKUP_SCHEMA_VERSION`; the version bumps in the same commit that changes the shape.
- Each format version needs its own frozen type, hand-written fixture and compatibility test before the constant moves; gate on the file's `declaredVersion`, never on an array being empty.
- The category detach (`clearCategoryOnTypeChange`) runs on every version, unversioned; the recurring restore runs last inside the import transaction so it reads the file's own categories.
- From v3 on, every exported file is unreadable on any older installed build — `decodePayload` refuses an unknown version.
- Upload order carries the meaning: payload → read-back → manifest; `<name>.json.manifest.json` existing states the payload was verified. Holds only because the bucket forbids `update` and uploads use `upsert = false`.
- The sidecar name appends the extension (`.json.manifest.json`) — the bucket's mime check is a verbatim string match; send bare `application/json`, never with a charset parameter.
- `SupabaseBackupObjectStore.upload`'s `.json` guard stays `DomainException.Unknown`, and the branch is unreachable: both keys come from `backupSnapshotName`/`manifestNameFor`, `BackupSnapshotNameTest` pins `BACKUP_MANIFEST_SUFFIX` and both name literals, so the rename that would trip it turns `:data` red before any device sees it — and the guard's message reaches Crashlytics carrying the offending key. Do not spend an exported `BackupFailureReason` case on it (E01-25, closed on this).
- The generation in a snapshot name is decorative: the writer stamps the live `BACKUP_SCHEMA_VERSION`, the matcher accepts any `backup-v<n>-`. What a payload *is* comes from its own `schemaVersion` key. Sharing one constant between the two makes every already-uploaded snapshot invisible to prune and verify the moment the constant moves.
- `storage.protect_delete()` refuses direct deletes; every delete goes through the Storage API. Resumable upload is unavailable, not merely unused.
- Retention slots fill from the data, never today's calendar — a slot is one distinct day/week/month that actually holds a snapshot; storage bound is `7 + 8 + 12` per shelf. `pinned/` is never scanned.
- A prune that deletes on a read failure is worse than one that skips: any read failure aborts before the first delete; individual failed deletes are recorded and never stop the run.
- An orphan payload (no manifest) is deleted on sight; a complete-looking pair with a bad manifest is kept — any pre-restore check must walk back to the newest snapshot that verifies.
- iOS reads the marketing version from `CFBundleShortVersionString` and falls back to `"unknown"`, never to a version-shaped guess — the value is stamped into the payload and its manifest, so a wrong version misattributes the file rather than admitting the gap.
- `Storage.Config.transferTimeout` (120s) bounds every Storage call including list/delete, not the 10s Postgrest `requestTimeout`; neither is configured in `SupabaseModule`.
- Whatever asks "what day is it" takes an injected `Clock` and `TimeZone`, no default (`docs/CODE_QUALITY.md`, Dates). Staleness needs both `BACKUP_STALE_AFTER_DAYS` (3 calendar days) AND a ledger that moved since the last verified snapshot.
- `hasLocalChangesSince` in `:domain` is the single predicate for "is there anything to back up" — the cycle gate and the UI warning must not be able to disagree.
- Account inheritance: one SQLite file per device, no wipe on user change — the next account inherits the previous one's rows, and snapshot backup would upload that ledger to the new account's bucket.
- No read query filters by `userId` (`all:`, `completeTransactions:`, `liveTotals:`, `getAccountBalance:`, `monthlyStats:` filter only `deletedAt IS NULL`) — same hazard as account inheritance, from the read side.
- `updateFromRemote` stamps `userId` unconditionally and the 23 seeded default categories carry identical UUIDs on every install, so the same PKs exist on every device that ever ran the app.
- RLS is correct, preserve it: `with check (user_id = (select auth.uid()))` on all four tables, `to authenticated` only; `delete_account()` is `security definer` with `set search_path = ''`, revoked from `public`/`anon`; the `backups` bucket policy is modelled on it.
- The bucket's RLS has exactly one automated proof, `supabase/tests/backups-rls-probe.sh`, run by the `probeStorageRls` workflow on `supabase/**` changes and by hand against a `supabase start` stack. It stays out of `qualityGate` deliberately — the gate must remain runnable with no Docker and no network. Every refusal it asserts is paired with the identical request from the owner, which succeeds: drop the owner-side assertions and what is left passes against a bucket carrying no policies at all.
- The shared `RemoteWriteMutex` bounds only the acquisition — 30s, then `DomainException.Busy`, and a caller that gives up holds nothing. It never bounds the work under the lock: account deletion is irreversible once its `delete_account` RPC returns, so a timeout able to cancel it mid-flight would trade a hang for local rows tagged with a userId the server no longer has. Holders: `DeleteUserAccountUseCase` and `BackupOrchestrator`'s upload-plus-watermark section.
- The deletion path resolves the session once, in `DeleteUserAccountUseCase.resolveAuthenticatedUserId`, and `DefaultAuthRepository.deleteAccount` deliberately does not repeat it: that use case is its only caller and needs the userId anyway for the sweep. A second bounded wait inside the repository would put 20s of session resolve inside the mutex's 30s acquisition, so the guard stays where the userId is resolved — move the wait and the RPC loses it.
- Production forensics: Supabase project `pievwpleqmrjwszuuivr` — zero rows with a non-null `deleted_at` anywhere; one tenant's rows point at the other's account/category PKs because the public schema has no foreign keys (ADR 001) — the "proven garbage" cloud cleanup truncates.
- The Perfil day count does not refresh across midnight, computed per emission without a ticker (same as `ReportUiState.isCurrentMonth`).
- Session and health are two flows — across an account switch there is one emission where the new session pairs with the old account's watermark (a display seam, not a recording one).
- Settled, do not reopen: shared accounts, multi-device sync, CRDTs, server-side LWW/conditional upsert, push-only row sync, or E2E encryption now (a Keystore-bound key dies with the phone).
- An unparseable `lastConfirmedPeriod` degrades the field to null, never the row — a template with no readable mark is still usable, unlike an unreadable `type`/`frequency`, which leaves the row itself unusable.
- Verification is read-only and is not a backup cycle: it never deletes — **not even an orphan payload, which prune deletes on sight** — never touches the watermark, the failure streak or published health, and emits no `BackupEvent`. Two tests pin the no-write property from independent angles.
- `isNewestPair` is measured against the newest parseable snapshot name, orphan or not. Filtering orphans before indexing makes a walked-back result claim it is the newest — precisely in the window a failed manifest PUT creates, which is the only reason orphans exist.
- A list or download failure is an error, never `NothingVerified` — collapsing them renders a dead network as "none of your backups verify".
- The verify phrase must fit the snackbar it is reported in. `EmmSnackbar` draws `maxLines = 2` with
  `TextOverflow.Ellipsis`, and `toPhrase()` appends clauses in table order, so the tables added last
  are the first the ellipsis eats. Six clauses already run ~127 characters, ~151 with the
  `Verificado un respaldo más antiguo` opening. ADR 009 Decision 4 makes per-table counts part of
  the ship gate, and a count behind an ellipsis is not a reported count — so this blocks flipping
  `SNAPSHOT_BACKUP_ENABLED`, not any one ticket. No UI test harness exists to catch it.
