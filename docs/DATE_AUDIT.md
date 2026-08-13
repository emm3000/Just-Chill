# Date handling — audit tracker

End-to-end audit of dates, from the picker down to the column, run on 2026-08-10. Thirteen findings.
This file is the tracker; the reasoning for each fix lives in its PR.

All thirteen are closed. One follow-up survives, recorded in full below: the sync wire and the
Supabase column still carry the occurrence as epoch millis (#5 — changing a column on a live server
is a human step, and nothing on the device depends on it any more). #7's sweep is done: no `Clock`
or `TimeZone` parameter anywhere in `:domain` or `:presentation` defaults to an ambient read.

Status legend: `[x]` closed · `[~]` partially closed · `[ ]` open.

## P0 — visible to the user

- [x] **1. The date picker never received the selected date.** Both screens passed
  `DateUtils.currentDateInMillis()`, so the calendar always opened on today — editing a March
  transaction opened on the current month. Root cause: the UiState carried a formatted label while
  the value lived in a `private var` on the ViewModel. → **#71**
- [x] **2. The day was resolved when the screen opened, not when it saved.** A screen opened at
  23:59 and saved at 00:01 booked the movement on the previous day, silently, with the chip still
  reading "Hoy". → **#72**
- [x] **3. Two Spanish month tables that disagreed.** `MonthLabels` spelled September "Setiembre"
  where `SpanishDateFormat` — golden-tested against `es-PE` — spelled it "septiembre". Both reached
  the screen. → **#71**
- [x] **4. `EditTransactionUiState` defaulted to a different date format** ("10 de agosto de 2026"
  where every other path rendered "Hoy"). Died with `DateUtils`. → **#71**

## P1 — modelling

- [x] **5. The model confused an instant with a calendar day.** `transactions.date` was epoch millis —
  an instant — but the domain treated it as a day. This did not break on one device. It broke with
  **sync**: a device in Lima records "10 Aug 23:30", a device in Madrid pulls it and shows 11 Aug,
  and at a month boundary it moves between months and changes the Reporte. LWW does not help — it is
  not a conflict, it is the same instant rendered differently.

  **Closed by collapsing the two representations into one, not by adding the second one.** The
  suggestion recorded here was to store the calendar day *alongside* the instant — that was tried,
  and it is what produced the defect that made this rewrite necessary: two fields that had to agree,
  kept agreeing by hand at each write site, until a fix to one end relocated the corruption onto the
  other. The lesson is in the fix: `transactions.occurredAt TEXT` is a `LocalDateTime` with no
  timezone and no companion field, so the desync is not something to remember — it is inexpressible.

  Schema v4 (`3.sqm`) rebuilds the table and converts the stored instant at a fixed America/Lima
  offset. Month windows, ordering and day grouping are now plain string operations on
  `'YYYY-MM-DDTHH:MM:SS'`. See `MigrationV3ToV4Test` and `TransactionDateEndToEndTest`.

  **Phase two, still open:** the sync wire and the Supabase column still carry `date bigint`.
  `data/shared/FixedPeruOffset.kt` converts both directions at the same fixed offset in the
  meantime — an interim adapter, deleted when the server column becomes text.

  **Confirmed by the sync audit (2026-08-13):** `FixedPeruOffset` and `OccurredAtText` round-trip
  losslessly — same fixed UTC-5 offset both directions, second precision — so today's `date bigint`
  value is not itself corrupt; it is the *stored* server value that misrepresents the instant.
  Phase two's eventual server-side conversion must therefore use **UTC-5, not
  `AT TIME ZONE 'UTC'`**: migration `3.sqm` already converted the historical local data under the
  UTC-5 assumption, and the server-side conversion has to agree with it or the two disagree on the
  same instant. Full reasoning: `docs/archive/sync/AUDIT.md`, §5 Protocol table.

- [x] **6. A global clock hidden in entity constructors.** `currentTimeInMillis()` was called from
  **19 sites**, including the default arguments of `TransactionInsert`, `AccountUpsert` and
  `Transaction.Empty`. An entity that reads the wall clock on construction is doing hidden I/O.

  The timestamps were **removed** from both entity types rather than threaded through: they are
  storage metadata, and `:data` already stamped them at the write on the `update` and `softDelete`
  paths — only `insert` trusted the caller, so the layer disagreed with itself about who owned the
  column. `:data` now takes an injected `Clock` and reads it once per write.
  `currentTimeInMillis()` is gone. → commits `f7b493b`, `6d10a8c`

  **That last sentence was not true when it was written, and the exception outlived it.**
  `DefaultBackupRepository` kept calling `Clock.System.now()` directly while its five siblings
  (`AccountLocalDataSource`, `CategoryLocalDataSource`, `TransactionLocalDataSource`,
  `RecurringMovementLocalDataSource`, `DefaultRecurringMovementRepository`) all took
  `private val clock` — and it was the writer where it mattered most, because an import re-stamps
  `createdAt`/`updatedAt` on **every** row the user owns, in one transaction, and those stamps are
  what LWW arbitrates on when the restore reaches another device. It was found by the #7 sweep,
  which was looking for something else. It now takes a `clock` in last position like the rest and
  reads it once through `Clock.nowMillis()`; `DefaultBackupRepositoryImportTest` pins what an import
  writes, across all three tables and across two imports at two instants. The rule this finding
  states now holds for every writer in the module, with no exception.

- [x] **7. The timezone was ambient where the clock was injected.** `YearMonth.startInclusiveMillis`,
  `GetHomeDataUseCase` and the `TransactionUi` read path all resolved days through
  `TimeZone.currentSystemDefault()`. The clock could be faked in tests; the zone could not, so no
  test could cover a month boundary in another zone.

  Most of it did not need closing so much as deleting: with #5 fixed, `YearMonth` and the read path
  resolve no days at all — the value already is one. What is left is the single question that
  genuinely needs a zone, "what is today for this user", and it is asked at two grains: which
  calendar day it is, and which month. Home and Movimientos now take the injected zone for **both**
  — `GetHomeDataUseCase`, `HomeViewModel`, `SeeTransactionsViewModel`, both transaction write paths
  and the frequency windows. They briefly took it only for the day, which meant a test could vary
  the zone and the month window would not notice; `YearMonth.current`'s `timeZone` parameter
  defaults to the ambient zone, and the default was being taken.

  **Reporte closed last, and the interesting part is what it cost.** `ReportViewModel` now takes an
  injected `clock` and `zone`, as `HomeViewModel` and `SeeTransactionsViewModel` already did, and
  its four `YearMonth.current()` calls take both. Reporte was also the first place where the two
  parameters carried **no defaults at all** — the rule the sweep at the end of this finding then
  applied to the rest of the codebase.
  Injecting alone would have been cosmetic: `GetSavingsRateUseCase` and `GetTopCategoriesOverMonthsUseCase`
  each accepted a `Clock` and then resolved the window's end month through the ambient zone anyway,
  so a test could fake the clock and still not say which month a report covered. Both now take a
  `zone` beside the `clock` — and **neither parameter keeps a default**. An ambient default is the
  defect, not a convenience: it lets a caller read the machine without saying so, which is precisely
  how Reporte drifted while every other caller was migrated. Deleting the defaults turned the silent
  drift into a compile error naming the two call sites.

  The wall-clock `UiState.month` defaults went with it — all three of them, in `ReportUiState`,
  `HomeUiState` and `SeeTransactionsUiState`. A default that reads the clock is the same bug at
  rest: it answers "which month is it" from the machine, at construction, for whoever forgot to
  say. Deleting them made the compiler name every construction site; each now states a month — a
  literal in tests and previews, the ViewModel's injected value in production.

  `ReportScreen` also stopped computing "is this the current month" itself — Compose has no
  injected zone to ask, so the `TodayPill` could disagree with the month printed beside it. Moving
  the answer into `ReportUiState.isCurrentMonth` cost something that is worth writing down: the
  Compose version was re-evaluated by recomposition for free, and a flag stored in state is not.
  Written once at open, it would have kept calling August the current month after midnight on
  1 September and kept the pill — the only one-tap way back — suppressed for the rest of the
  session. So **every** path that writes state re-derives it, both reloads included, not only a
  user-initiated month move.

  The honest limit, and it applies to **two** markers, not one: `ReportUiState.isCurrentMonth`
  behind the pill, and `MonthlyBarItem.isCurrentMonth`, which draws one bar of the Tendencias chart
  bold. Both come off a clock read taken per load, so both refresh together and both go stale
  together — refreshing on every state write is not the same as refreshing continuously. A screen
  sitting idle with nothing loading will not notice a rollover until the next intent arrives.
  Reporte has no resume hook to hang a refresh on; adding one is a separate change. Each marker has
  its own test that moves a clock across a boundary under a running ViewModel, because a limit
  disclosed for only one of its two symptoms is how this file misled a reader once already.

  What proves it: one instant, two zones, two different months. `2026-09-01T02:00Z` is already
  September at UTC and still 31 August at UTC-5, and `ReportViewModelTest`,
  `GetSavingsRateUseCaseTest` and `GetTopCategoriesOverMonthsUseCaseTest` each assert **both** — one
  zone proves nothing, because on a machine sitting in it the ambient read agrees by accident and
  the test stays green straight through the bug.

  **The sweep that followed, and what it found.** The rule above held for `ReportViewModel` and the
  two report use cases; everywhere else the same parameters still fell back to `Clock.System` /
  `TimeZone.currentSystemDefault()`. Fourteen declarations lost twenty-five defaults between them:
  `YearMonth.current` and `YearMonth.of(epochMillis)` — the root, since every other default
  ultimately reached them — `startOfDayDaysAgo`, `GetHomeDataUseCase`, `CreateTransactionUseCase`,
  `UpdateTransactionUseCase`, `GetFrequentCombosUseCase`, `GetTopUsedCategoryIdsUseCase`,
  `GetPendingRecurringMovementsUseCase` (zone only), `HomeViewModel`, `SeeTransactionsViewModel`,
  `AddTransactionViewModel`, `EditTransactionViewModel` and `ProfileViewModel` (clock only).
  `GetHomeDataUseCase.invoke(yearMonth = YearMonth.current(clock, zone))` keeps its default: that
  one is computed from the injected pair, which is the shape the rest was moved to, not the defect.

  **Twenty-four of the twenty-five were never evaluated in production**, so deleting them changed
  nothing at runtime: Koin's constructor DSL ignores Kotlin defaults and injects every time, and the
  three plain functions were already called with every argument spelled out.

  The twenty-fifth *was* evaluated, and it is worth being exact about what that did and did not
  mean. `ProfileModule` builds `ProfileViewModel` by hand (its `appVersion` is a qualified `String`
  the constructor DSL cannot resolve by type) and never passed a clock, so `ProfileViewModel.clock`
  came from its `= Clock.System` default rather than from the graph. **No wrong value was ever
  produced.** `SharedModule` binds `factory<Clock> { Clock.System }` — the same stdlib singleton —
  so `exportedAt` is byte-identical before and after this change, and no backup file on anyone's
  disk needs a second look. Reached is not the same claim as wrong, and this file should not blur
  them.

  What the deletion bought is a *guarantee*: omitting the clock in that block is now a compile
  error. The trap it closes is the day someone rebinds `Clock` in `SharedModule` — to an offset
  clock, a server-synced one, anything — and Profile alone keeps reading `Clock.System` while every
  other consumer moves, silently, because a default answered for it.

  A compile error is only a guarantee for as long as no default comes back, though, and it covers
  nothing for a dependency that never had one to lose. So the property is now **tested**, in
  `AppGraphKoinTest.every graph-built class holds the Clock and TimeZone the graph bound`: it
  rebuilds the real graph with a sentinel clock and a sentinel zone bound, resolves every
  definition, and asserts by identity that every `Clock`/`TimeZone` field on a `com.emm.` class
  holds the bound instance — 28 fields today. Identity and not equality is the whole point: two
  `Clock.System` references are equal, and only `assertSame` separates "injected" from "defaulted".
  It generalises past the two hand-written blocks it was written for, so converting any class to a
  hand-written `viewModel { }` later inherits the guard.

  Reproducing the original defect proves it: restore the `= Clock.System` default, delete
  `clock = get()` from `ProfileModule`, and that one test fails with
  `ProfileViewModel.clock holds kotlin.time.Clock$System instead of the bound instance` — while all
  828 other tests, `ProfileViewModelTest` included, stay green. That is the measure of the gap that
  existed: the whole suite passed through this bug for the life of the class.

  One thing that is **not** evidence for any of the above, and an earlier draft of this paragraph
  claimed it was: `ProfileViewModelTest.ExportRequested stamps exportedAt from the injected clock`.
  It is worth having — the export's timestamp had no test at all — but a Kotlin default never blocks
  an explicit argument, so it constructs the ViewModel directly, passes `clock = fixedClock`, and
  compiles and passes against the pre-change constructor too. Verified by restoring the default and
  re-running it, not by reasoning about it.

  The composition root is where reading the machine belongs, and `hh/di/SharedModule.kt` is now the
  only way a clock or a zone enters the injected graph: two factories, one `Clock.System` and one
  `TimeZone.currentSystemDefault()`, feeding every date question in `:domain` and `:presentation`.

  **What still reads the machine, named rather than counted.** This list has been wrong twice, both
  times because it carried a number that went stale while the code moved. There is no number now.
  It covers **production code only** — `androidHostTest`/`commonTest`/`test`/`androidDeviceTest`
  sources and the dev-flavor `androidApp/src/dev/.../experiences/` playground are outside it and
  read the clock freely; the playground is not part of the product.

  This command regenerates the list, and its output **is** the list — currently eight lines: the two
  `SharedModule` factories, `SyncOrchestrator`, and the five `:ui-android` lines making up the four
  deliberate entries below (`DatePickerSheet` accounts for two of them).

  ```
  rg -n --type kotlin \
    -e 'LocalDate\.now\(\)' -e 'LocalDateTime\.now\(\)' -e 'Instant\.now\(\)' \
    -e 'System\.currentTimeMillis' -e 'Clock\.System' -e 'currentSystemDefault\(\)' \
    -g '!build/' -g '!**/src/*[Tt]est*/**' -g '!androidApp/src/dev/**' \
    | rg -v ':[0-9]+: *(//|\*)'
  ```

  Two things to know before trusting it. The second `rg` drops lines whose first non-space character
  begins a comment, which is what keeps at least six KDoc mentions of `Clock.System` out of the
  output (`DayLabels`, `DayGroup`, `DefaultBackupRepository`, `ClockExtensions`, `DateExtensions`,
  `ProfileModule` — prose *about* ambient reads, not ambient reads). It is a heuristic, not a
  parser: a trailing comment on a code line would still show up. And an earlier revision of this
  file published a looser version of this command while calling it authoritative; run verbatim it
  returned sixteen lines for an eight-line list. Skim the output; do not paste the count anywhere.

  Deliberate, all in `:ui-android`, none with a value that outlives the screen:

  - `DatePickerSheet` — the calendar's "today" and the zone it dims future days in. A composable
    with no ViewModel behind it; the day it picks is passed down as a value, not read back.
  - `ProfileScreen`'s last-sync stamp — renders a real past instant and is *supposed* to move with
    the device.
  - `SeeTransactionsScreen`'s `@Preview` helper — never runs in the app.
  - `PlatformHostActions.suggestedExportFilename` — `justchill-backup-YYYY-MM-DD.json`, the name the
    SAF picker pre-fills. **The date the user is standing in is the correct answer here**, and the
    value never leaves the picker: the user can rename the file, and nothing derived from it enters
    the payload, the database or sync. Note it is an *independent* read from the `exportedAt` inside
    that payload, which comes from the injected clock — in production both are `Clock.System` in the
    device zone and agree, but they are two reads, not one, and only the payload's is pinned by a
    test. This module does have `koin-compose`, so the honest reason not to inject is that it would
    buy no assertion, not that it cannot be done.

  Not deliberate — one read, `SyncOrchestrator.runSync`, which stamps `lastSyncedAt`. **It is not a
  date defect.** It resolves no calendar day and consults no timezone, so nothing in this finding
  applies to it: it is an instant, and it is rendered as one. What it is, is injection debt — a
  writer that could take a `Clock` and does not, so no test can pin the value it records. Listed
  here because it is the last ambient read outside `:ui-android`, not because it is a timezone bug.
  Do not "fix" it as one.

  `DefaultBackupRepository.importFromJson` used to be on that second list. It is gone — see #6.

- [x] **8. A "last 90 days" window measured in fixed milliseconds.** `now - days * 24h` is a
  duration, not a number of days; it drifts by an hour across a DST change and starts mid-morning
  otherwise. Duplicated in two use cases. → **#74**

## P2 — consistency and hardening

- [x] **9. Deprecated `kotlinx.datetime.Instant`** in four `data/sync` files while the rest of the
  repo used `kotlin.time.Instant`. → **#74**
- [x] **10. `Instant.parse` on server input with no guard**, inside the per-page database
  transaction. The class guarded a *missing* `server_updated_at` but not an unreadable one, so a
  malformed value took down the whole pull. → **#74**
- [x] **11. Nothing guards a future date.** The picker navigated forward without a limit and the
  schema accepts anything, so a transaction dated in 2030 entered the balance and captured
  `lastUsedAccountId`, which orders by `date DESC`.

  The rule lives in `TransactionDateRules.ensureNotFutureDated`, called by both write paths after
  the date is combined — it belongs to the transaction, not to creating or editing one. It compares
  calendar days, not instants, because the Edit path keeps the original transaction's time of day.
  The picker mirrors it as an affordance (future days dimmed, forward chevron stopped at the current
  month) but does not own it. → commits `01990d9`, `408ef4c`
- [x] **12. `DateUtils` was a static object reading the ambient clock and zone**, unreachable from
  tests and living under `hh.transaction` while half the app used it. Deleted; its clock-free parts
  are `hh/shared/DayLabels.kt`. → **#71**
- [x] **13. Stale documentation on `SpanishDateFormat`** — it pointed at a test in `:app`, a module
  that no longer exists, and described a Compose Multiplatform setup that was retired. → **#74**

## What was already right

Worth keeping in view, because these are the patterns the fixes above copied:

- `YearMonth` / `MonthRange` — month bounds are half-open and computed in Kotlin, never in SQL, with
  the comment explaining why that matters for anyone off UTC. (Since #5 they need no timezone at
  all: the bounds are ISO day strings.)
- `DayGroup` — takes `today` as an input rather than reading a clock. This is the shape the rest of
  the date code was moved to.
- `RecurringDueRules.periodKey` — zero-padded so string order matches chronological order, and
  `parsePeriodKey` treats the stored value as untrusted because it arrives from other devices.
- `ConflictResolver` — does not consult a clock where it does not need one, and says why.

`DateAndTimeCombiner` used to be listed here — idempotent, well-documented, tested on both sides of
UTC. It was all of that and it still could not be safe, because its job was to hold two
representations of one fact in agreement. It is gone; the job does not exist any more.
