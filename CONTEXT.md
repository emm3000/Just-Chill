# JustChill

Local-first personal finance app for one person in Peru: manual capture of income and spending in
soles in seconds, a month report, informal loans kept apart from the ledger, and an opt-in snapshot
backup. The device is the source of truth; nothing needs an account or a network.

## Language

### Ledger

**Transaction**:
One recorded movement of money on an Account: an Income or a Spend with an Amount, an optional
Category, a description and the local date and time it occurred.
_Avoid_: entry, record, expense row, item, recurring movement, variable amount, period, pending, paused
_UI term_: "movimiento"

**Income**:
A Transaction type: money that came in. Its Amount is tinted positive on screen.
_Avoid_: credit, earning, deposit
_UI term_: "Ingreso"

**Spend**:
A Transaction type: money that went out. Its Amount stays monochrome on screen, never red.
_Avoid_: expense, debit, outflow
_UI term_: "Gasto"

**Amount**:
A quantity of soles held in cents, always a magnitude. The Transaction type carries the sign.
_Avoid_: value, price, sum, total (reserved for Loans)

**Occurred at**:
The local wall-clock date and time the user says a Transaction happened. It carries no time zone, so
"which day" never depends on where the reader is.
_Avoid_: timestamp, created at, date, instant

**Account**:
A named place money sits: bank, cash, credit card, investment or wallet. It has no opening balance,
so it has no lifetime balance; every per-Account figure is a Net for one Month.
_Avoid_: wallet (as the generic), balance, user account (see Identity)
_UI term_: "Cuenta"

**Category**:
A named label with an icon and a colour name, typed Income or Spend. A Transaction carries one
Category of its own type, or none.
_Avoid_: tag, label, group, class

**Uncategorized**:
A Transaction carrying no Category. It is a normal state, not an error.
_Avoid_: other, misc, unfiled
_UI term_: "Sin categoría"

**Amount pad**:
Where a Transaction is typed and the app opens: the Amount first, with the type, Account,
Category and date taking defaults the user can override before saving.
_Avoid_: calculator, add screen, entry form, capture form

**Frequent combo**:
An Account, a Category and a Transaction type the user records together often, used to preselect
the amount pad's account and category.
_Avoid_: favourite, preset, template, quick add

**Month**:
A calendar year-month, the window every total, report and Account figure is scoped to.
_Avoid_: period, cycle, range

**Filter**:
Free text, a set of Categories and an optional amount range that narrow the Transaction list. Empty means everything.
_Avoid_: search, query, criteria

### Loans

**Loan**:
Money lent to a Person, kept in a parallel ledger: it never posts a Transaction, moves no Account and
never appears in the Month report. The user records the cash movement separately if they choose.
_Avoid_: debt, credit, transaction, advance
_UI term_: "Préstamo"

**Person**:
Whoever owes a Loan, identified by the name the user typed. There is no contact record.
_Avoid_: contact, borrower, debtor, client, counterparty

**Person key**:
The normalized form of a Person's name (case, accents and spacing folded) that groups every Loan to
the same Person. Two people whose names fold alike share one row; that cost is accepted.
_Avoid_: id, slug, handle

**Principal**:
The Amount lent.
_Avoid_: amount (ambiguous here), capital, base

**Interest**:
A flat percentage of the Principal, in basis points, frozen when the Loan is created. It never accrues
and produces no schedule.
_Avoid_: rate, APR, yield

**Total due**:
Principal plus Interest, computed once at creation and stored.
_Avoid_: total, balance, amount owed

**Payment**:
A partial or full repayment of one Loan, in cash or by transfer. It is a return of capital, never an
Income.
_Avoid_: transaction, income, instalment, refund, abono (in code)
_UI term_: "Abono"

**Remaining**:
Total due minus the Payments so far, for one Loan.
_Avoid_: balance, outstanding, debt, left

**Person balance**:
The sum of Remaining across every Loan to one Person.
_Avoid_: debt, total owed, exposure
_UI term_: "Por cobrar"

### Report

**Monthly totals**:
Income, Spend and Net for one Month.
_Avoid_: summary, stats, figures

**Net**:
Income minus Spend for one Month, signed. Positive is tinted; zero or negative stays monochrome.
_Avoid_: balance, savings, profit, result

**Savings rate**:
Net as a percentage of Income for one Month, with its change in points against the prior Month.
_Avoid_: ratio, margin, saving percentage

**Monthly comparison**:
A Month's total against the previous Month's, as a percentage and an absolute Amount.
_Avoid_: trend, delta, variation

### Backup

**Backup**:
The opt-in copy of all local data to the signed-in Identity's cloud storage, taken as Snapshots. It
is one device at a time: no row replication, no merge between devices.
_Avoid_: sync, replication, cloud save
_UI term_: "Respaldo"

**Snapshot**:
One complete, versioned export of every table at a point in time, uploaded as a single file.
_Avoid_: backup (the feature), dump, diff, delta

**Export**:
Producing a Snapshot's content from the local database.
_Avoid_: sync, upload (the transport), dump

**Restore**:
Loading a Snapshot into the local database and reporting how many rows of each kind landed.
_Avoid_: sync, import (in prose), merge, download

**Local change**:
Any write to the local database after the last successful Backup. It is the single predicate for
"is there anything to back up"; the cycle and the warning on screen read the same one.
_Avoid_: dirty flag, pending changes, unsynced

**Stale**:
A Backup whose last Snapshot is more than three days old while a Local change exists.
_Avoid_: outdated, expired, behind

**Retention**:
Which Snapshots survive pruning: the newest per day for seven days, per week for eight weeks, per
month for twelve months.
_Avoid_: rotation, cleanup, expiry, TTL

**Verification**:
Reading the newest uploaded Snapshot back with its row counts before it counts as a Backup.
_Avoid_: validation, checksum, integrity check

**Backup destination**:
The Identity whose storage receives the Snapshots. One phone keeps one database across Identities,
so a changed destination is disclosed on screen before its first upload.
_Avoid_: tenant, target, remote, bucket

**Erase**:
Deleting every uploaded Snapshot an Identity owns, as part of deleting that Identity.
_Avoid_: purge, clear, wipe (local data is never wiped)

### Identity

**Identity**:
The optional email-and-password or Google login that enables Backup. The app is complete without
one, and the state "no Identity" is permanent and supported.
_Avoid_: account (reserved for the money container), user, user account, profile

**Session**:
The signed-in state of an Identity on this device. Signing out ends it; local data survives.
_Avoid_: login, token, auth state

**Confirmation pending**:
A freshly created Identity that cannot sign in until the emailed link is opened.
_Avoid_: unverified, unconfirmed user, waiting
