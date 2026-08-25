# E06 — Navigation shell

## Why

The bottom bar's "Inicio" tab is the author's least-used destination, while the per-category report
is their second-most-used — and the report sat behind Inicio as its only door. Home was also largely
a worse copy of two other screens: its hero balance and income/spend row duplicate what
`ReportScreen` already renders, and its recent-transactions list duplicates the whole "Ver" screen.
Of Home's six sections only two were irreplaceable, and both only because they were sole doors. This
epic re-shapes the shell around what is actually used.

## Constraints

- **A feature reachable through exactly one door is unreachable the moment that door moves.** This
  has now bitten twice: `LoansCard` was the sole push of `LoansRoute`, and `HomeEntries` was the sole
  push of `ReportRoute`. `ui-android/CLAUDE.md` already records the first. When adding a destination,
  give it a door that does not depend on a screen the author does not open.

- **The bottom bar holds four tabs plus the centre add button, and no more.** `HhBottomBar` already
  carries a comment recording that its 10sp labels sit under the 4.5:1 AA floor; a fifth tab shrinks
  them further. Adding a destination means swapping one out, not appending.

- **Promoting a route to a tab means changing its supertype, and every tab route is restored
  reflectively.** A `BottomBarRoute` is re-resolved through `Class.forName(name).kotlin.serializer()`
  on process-death restore, so it must be `@Serializable` and must appear in `RouteSerializationTest`.
  The compiler catches neither.

- **A screen promoted from pushed-detail to tab loses its back affordance.** `ReportTopBar` took an
  `onBack`; a tab has nothing to go back to. A leftover back button on a tab pops the start
  destination.

- **`SeeTransactionRoute` is the start tab, and iOS no longer disagrees.** ADR 003's open item 6
  recorded a divergence where `PlatformHostActions.ios.kt` set `HomeRoute` while Android set
  `SeeTransactionRoute`; that iOS file no longer exists (`ui-android/src` holds only `androidMain`
  and `androidHostTest`), and this epic settles the question on the Android side. Do not reintroduce
  a second start tab when iOS thaws.

- **A screen that recomputes another screen's numbers is the liability this epic is paying off.**
  Home held income, spend and balance that `ReportScreen` also derives, and a transaction list that
  `SeeTransactionsScreen` also renders. Before adding a section to any shell screen, check whether an
  existing screen already owns that number.

- **The catch-up marker on a pending recurring row keeps `danger` on purpose (E06-08).**
  `PendingRecurringComponents.kt` and `ConfirmRecurringSheet.kt` tint an overdue period label
  `danger`, never an amount — an overdue recurrente is a broken state, which `danger` is for, and
  `DESIGN_SYSTEM.md` §4 governs amounts, not labels. Do not sand this down as leftover red.

- **`SavingsRateBlock`'s rate number keeps `danger` on a deficit; its trend pill does not (E06-08).**
  A negative savings rate is a broken period, not a spend amount, so the rate stays `danger`. The
  trend-delta pill beside it is a comparison, not the deficit itself, so a negative delta uses
  `PillTone.Neutral`. Both calls are deliberate — do not unify them.

- **Ver's browsed month follows the calendar only until the user picks a different one (E06-06).**
  `SeeTransactionsViewModel`'s month-rollover collector advances `selectedMonth` exactly when it
  still equals the calendar month recorded at the previous `today` tick — always advancing would drag
  a deliberately browsed month back to today's; never advancing would leave an app left open past
  midnight stuck on yesterday's month. `SeeTransactionsViewModelTest`'s two rollover tests pin both
  directions; don't "simplify" the check to an unconditional jump.
