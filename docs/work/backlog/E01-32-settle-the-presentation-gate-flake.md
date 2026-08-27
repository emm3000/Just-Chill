# E01-32 — Settle the `:presentation` gate flake

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `:presentation:testDebugUnitTest` either reproduces `UncaughtExceptionsBeforeTest` under a
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

A second sighting during E06-06 points elsewhere: twice, only in invocations that also ran detekt,
with the suppressed cause a cancelled `Dispatchers.Main` coroutine plus a cancelled `Dispatchers.IO`
one — the shape of `TransactionDateEndToEndTest`'s leaked ViewModel coroutines, not supabase-kt's.
It did not recur in 20+ later runs. Rule that suspect in or out before chasing the reported one.
