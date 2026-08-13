package com.emm.justchill.core.sync

/**
 * Kill switch for the whole sync feature. `true` — the current value — means sync is OFF.
 *
 * Why it exists: the push dropped every row carrying a stale `userId` while `countPending` kept
 * counting them, so the debounced-writes trigger re-fired every few seconds forever, and the cycles
 * that did reach Supabase wrote dangling cross-tenant rows. The app is local-first, so switching
 * sync off costs nothing and stops the corruption.
 *
 * **This stays `true` forever.** `docs/adr/009-backup-is-a-snapshot-not-row-replication.md` replaces
 * row replication with snapshot backup and deletes this engine rather than repairing it, so there
 * is no "turn sync back on" any more — flipping this to `false` would re-enable the loop above.
 * The constant and its gates are removed in the last phase of `docs/sync/ADR009_PLAN.md`, once
 * nothing references the engine. Grep this name to find every site it gates.
 */
const val SYNC_TEMPORARILY_DISABLED: Boolean = true
