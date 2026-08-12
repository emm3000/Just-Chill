package com.emm.justchill.core.sync

/**
 * Kill switch for the whole sync feature. `true` — the current value — means sync is OFF.
 *
 * Why it exists: the push dropped every row carrying a stale `userId` while `countPending` kept
 * counting them, so the debounced-writes trigger re-fired every few seconds forever, and the cycles
 * that did reach Supabase wrote dangling cross-tenant rows. The app is local-first, so switching
 * sync off costs nothing and stops the corruption. The sync layer is under redesign —
 * see `docs/sync/PLAN.md`.
 *
 * Temporary. To turn sync back on, flip this to `false`: nothing was removed, every Koin binding,
 * test and engine class is still wired. Grep this name to find every site it gates. Delete the
 * constant when the redesign lands.
 */
const val SYNC_TEMPORARILY_DISABLED: Boolean = true
