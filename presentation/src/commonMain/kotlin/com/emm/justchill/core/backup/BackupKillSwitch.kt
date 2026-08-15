package com.emm.justchill.core.backup

/**
 * Kill switch for snapshot backup. `false` — the current value — means the pipeline is OFF.
 *
 * **The polarity is inverted from `SYNC_TEMPORARILY_DISABLED`, and both constants mean "off" today.**
 * That one is `true` for off because it names a disabling; this one is `false` for off because it
 * names a capability. Reading either as a bare boolean without its name is how the two get confused —
 * `if (!SYNC_TEMPORARILY_DISABLED)` and `if (SNAPSHOT_BACKUP_ENABLED)` are the same test.
 *
 * Why it exists: `docs/adr/009-backup-is-a-snapshot-not-row-replication.md` replaces row replication
 * with a versioned JSON snapshot uploaded to Supabase Storage, and the pipeline is built in pieces
 * across `docs/sync/ADR009_PLAN.md`'s Phase 2. It ships dark so each piece can land, be reviewed and
 * be gated without ever writing to a real bucket. **This flips to `true` only when Phase 4 passes** —
 * the CI round-trip, the in-app verify, the documented restore drill and the automated RLS probe.
 * Nothing short of all four; the flag is what stops a half-proven restore path from being the thing
 * a user's whole ledger depends on.
 *
 * Grep this name to find every site it gates.
 */
const val SNAPSHOT_BACKUP_ENABLED: Boolean = false
