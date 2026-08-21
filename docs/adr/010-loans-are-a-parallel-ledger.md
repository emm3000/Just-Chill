# ADR 010 — Loans are a parallel ledger

- **Status**: Accepted
- **Date**: 2026-08-21
- **Deciders**: Edgardo Muñoz
- **Amends and supersedes**: nothing. No W- row moves and none is added — `PRODUCT_REQUIREMENTS.md`
  §1 has never carried a row about lending. The Won't-have table runs W-01, W-04–W-10 and W-12, plus
  the opt-in W-02/W-03/W-11; none mentions préstamos. Unaddressed scope, not rejected scope.

> Resumen (es): un préstamo es un **libro paralelo**. Prestar S/500 no crea un Spend y un abono no
> crea un Income: `loans` y `loan_payments` no tocan `transactions`, ni el saldo de una cuenta, ni el
> reporte del mes. La alternativa real — modelarlo como un par Spend/Income unido por un id —
> mantiene el saldo al día sin esfuerzo, y se rechaza porque **un abono no es ingreso**: es la
> devolución del capital. Meterlo al reporte infla los dos lados — el mes del préstamo parece un pico
> de gasto y el del cobro un ingreso extraordinario — y peor cuanto más grande es el préstamo. El
> costo aceptado, sin maquillaje: el saldo de la app no refleja la plata prestada hasta que el
> usuario registre el movimiento a mano. El interés es **plano y congelado al crear**, no devengado.

## Context

The author lends money informally and repeatedly, and repayments arrive piecemeal, in cash or by
transfer. Those balances live in their head or on paper today; the feature holds them instead.

The app has exactly one ledger: `transactions`. Every row carries a `type` that signs its `amount`,
and three aggregates read that sign — `getAccountBalance`, `liveTotals`, and the month report. A loan
is the only money the app would ever hold that is neither earned nor spent: it left the account and
is expected back. This ADR decides whether it enters that ledger, and what it carries once it does not.

## Decision

1. **A loan is a parallel ledger.** It never posts to `transactions`, never moves an account balance,
   and never appears in the month report. Lending creates no `Spend`; an abono creates no `Income`.
   `loans` and `loan_payments` are read by the loans screens and by nothing else — no Home total's
   query and no report query references either table. The user records the real cash movement as an
   ordinary transaction if and when they choose; the app neither prompts for it nor links the two.
2. **Interest is flat and frozen at creation.** `totalDue = principal +
   round_half_up(principal * interestBps / 10_000)`, computed once by `LoanMath` and stored. It does
   not accrue with time and no instalment schedule is generated. These are informal loans between two
   people who agreed on one number; an accruing rate is a different product, and the feature deleted
   in `d1d9446a` — `interest`, `duration`, a stored `status` and a daily instalment generator that
   skipped Sundays — is the evidence of what that one becomes.
3. **That rounding is HALF-UP, and deliberately not the repo's existing rounding.**
   `NumberFormatEs.integerRounded` rounds HALF-EVEN because it imitates
   `NumberFormat.getNumberInstance(es-PE)` for display; `LoanMath` rounds HALF-UP because it computes
   an amount that gets stored and that a person will be asked to pay. A display formatter and money
   math are different jobs — do not unify them.

## Acceptance criterion — `PRODUCT_REQUIREMENTS.md` §3

- **No está en §1** — verified against the table itself, row by row; see the header block above.
- **Le ahorra tiempo o le aporta claridad** — it replaces a per-person balance kept in the author's head.
- **No reduce la velocidad de registro de un movimiento** — loans are a separate surface and add
  zero taps to the add-transaction flow. This is the criterion Decision 1 most directly protects:
  wiring a loan into `transactions` would have put a loan concept inside the movement form.
- **Es descubrible: no requiere onboarding ni tutorial** — a Home card is the entry point, and no
  contacts CRUD stands between the user and their first loan: the person is a `personKey` column.
- **No pide data del usuario que la app no necesita** — a typed name, an amount, a date. No
  contacts permission, no phone numbers, and nothing leaves the device beyond the snapshot already
  opted into under [ADR 009](009-backup-is-a-snapshot-not-row-replication.md).

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Model the loan as a `Spend` and each abono as an `Income`, linked by a loan id** | The real alternative, and it keeps the account balance truthful with no user effort. Rejected because **a repayment is not income** — it is the return of capital. Folding it into the month report inflates both sides: the month of the loan reads as a spending spike, the month of the repayment as a windfall, and the report stops answering the question it exists to answer ("what did I earn and what did I spend"). The distortion is worst exactly when the loan is largest. |
| **Post to the balance, exclude from the report** | The middle option: real `transactions` rows, filtered out of the report by a marker column or a reserved category. It buys the truthful balance, but the exclusion is a rule every present and future aggregate has to remember, and the row still carries a `type` that misdescribes it. ADR 008's cost table is the precedent for how many writers such a convention has to survive. |
| **A "Préstamos" category pair instead of new tables** | Cheapest by far, and ADR 008 already forces two categories (one Income, one Spend) for exactly this name. A category is a label, not a ledger: it carries no counterparty, no principal/total split and no remaining balance — which is the whole feature — and the amounts land in the report anyway. |
| **Accruing interest with a generated instalment schedule** | What `d1d9446a` deleted. It turns an agreement between two people into a loan product: a due-date calendar, late rules, and a schedule that has to be kept in sync with piecemeal repayments that ignore it. |

## Consequences

### Positive

- The month report keeps answering its own question. Lending and being repaid do not move it.
- Nothing in the movement form changes, so §2's `<15s` registration budget is untouched.
- Both tables are additive and read by one screen set, so nothing can depend on them by accident.

### Negative / costs

- **The account balance does not reflect money that is out on loan**, until and unless the user
  records the movement themselves. A truthful report is chosen over an automatic balance, knowingly.
- **The report is truthful about earning and spending, not about cash.** If the user does record
  the cash movement as an ordinary transaction, the distortion above returns — nothing links the two
  entries and nothing detects the pair. That is the price of leaving the call to the user.
- **Interest earned never reaches the month report either, and interest genuinely is income.**
  Decision 1 treats every abono as return of capital, which is exactly wrong for the interest slice
  of it. Splitting an abono into principal and interest needs an amortization convention — the
  accruing product Decision 2 refuses. Accepted as understatement, not claimed as correctness.
- Nothing reconciles a loan against the account it came from: no check that a recorded abono
  corresponds to money that actually arrived.

## Notes

- The invariants outliving every ticket under this decision — derived settled-ness, `totalDue` on
  every write path, cascade soft-delete, `personKey`, the backup payload — are in
  [`docs/work/epics/E05-loans.md`](../work/epics/E05-loans.md).
- Decision 3 is money math; the display side is `NumberFormatEs` and its golden test.
