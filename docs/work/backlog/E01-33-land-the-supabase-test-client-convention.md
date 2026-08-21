# E01-33 — Land the Supabase test-client convention

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] the rule is written where a writer reads it before building a `SupabaseClient` in a test:
      `awaitInitialization()` between `createSupabaseClient` and `importSession`, because
      `initDone()`'s check-then-act on `sessionStatus` is not atomic and races the import
- [ ] the same place records that `Dispatchers.setMain`/`resetMain` are inert under
      `minimalConfig()` — `enableLifecycleCallbacks = false` makes the only `Dispatchers.Main`
      dispatch unreachable
- [ ] `SupabaseBackupObjectStoreTest`'s dead `setMain`/`resetMain` scaffolding is gone

## Context

The race was found and fixed in `SupabaseBackupObjectStoreTest` by `4359e11c`, then reintroduced a
week later in two new auth test files, because the knowledge lived only in a commit message. It has
bitten twice. Does not gate the flag.
