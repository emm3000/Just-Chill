# E01-32 — Settle the `:presentation` gate flake

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `:presentation:testAndroidHostTest` either reproduces `UncaughtExceptionsBeforeTest` under a
      written procedure, or the report is recorded as unreproducible and this ticket dies
- [ ] if it reproduces, the fix stops `AppGraphKoinTest` from leaking a late `Dispatchers.Main`
      dispatch past `resetMain()` — never a retry, a sleep or an `@Ignore`
- [ ] the closing commit states the run count and the method that settled it

## Context

Reported mechanism: `AppGraphKoinTest` builds the real graph, supabase-kt's `AuthImpl.init` launches
on `Dispatchers.Default` and reaches `Dispatchers.Main` via `setupPlatform`, landing after
`tearDown()` ran `resetMain()`; the `IllegalStateException` parks in kotlinx-coroutines-test's
global collector and fails whichever `runTest` starts next. Reported as 6/6 under tight `--rerun`
loops and 4/4 on a stashed clean trunk; six tight loops here passed 6/6. Does not gate the flag.
