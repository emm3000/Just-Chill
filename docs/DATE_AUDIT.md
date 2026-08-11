# Date handling — audit tracker

End-to-end audit of dates, from the picker down to the column, run on 2026-08-10. Thirteen findings.
This file is the tracker; the reasoning for each fix lives in its PR.

All thirteen are closed. One follow-up survives them, recorded under #5: the sync wire and the
Supabase column still carry the occurrence as epoch millis, because changing a column on a live
server is a human step. Nothing on the device depends on it any more.

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

- [x] **6. A global clock hidden in entity constructors.** `currentTimeInMillis()` was called from
  **19 sites**, including the default arguments of `TransactionInsert`, `AccountUpsert` and
  `Transaction.Empty`. An entity that reads the wall clock on construction is doing hidden I/O.

  The timestamps were **removed** from both entity types rather than threaded through: they are
  storage metadata, and `:data` already stamped them at the write on the `update` and `softDelete`
  paths — only `insert` trusted the caller, so the layer disagreed with itself about who owned the
  column. `:data` now takes an injected `Clock` and reads it once per write.
  `currentTimeInMillis()` is gone. → commits `f7b493b`, `6d10a8c`

- [x] **7. The timezone was ambient where the clock was injected.** `YearMonth.startInclusiveMillis`,
  `GetHomeDataUseCase` and the `TransactionUi` read path all resolved days through
  `TimeZone.currentSystemDefault()`. The clock could be faked in tests; the zone could not, so no
  test could cover a month boundary in another zone.

  Most of it did not need closing so much as deleting: with #5 fixed, `YearMonth` and the read path
  resolve no days at all — the value already is one. What is left is the single question that
  genuinely needs a zone, "what is today for this user", and every place that asks it now takes the
  zone next to the clock it already took: `GetHomeDataUseCase`, `HomeViewModel`,
  `SeeTransactionsViewModel`, both transaction write paths and the frequency windows.

  Still ambient, and deliberately: `DatePickerSheet` and the Compose previews, which have no
  injected clock either, and `ProfileScreen`'s last-sync stamp, which renders a real instant and is
  supposed to move with the device.

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

`DateAndTimeCombiner` used to be listed here — idempotent, well-documented, tested on both sides of
UTC. It was all of that and it still could not be safe, because its job was to hold two
representations of one fact in agreement. It is gone; the job does not exist any more.
- `RecurringDueRules.periodKey` — zero-padded so string order matches chronological order, and
  `parsePeriodKey` treats the stored value as untrusted because it arrives from other devices.
- `ConflictResolver` — does not consult a clock where it does not need one, and says why.
