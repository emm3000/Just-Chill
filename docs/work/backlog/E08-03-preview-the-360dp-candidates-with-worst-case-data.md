# E08-03 — Preview the 360 dp candidates with worst-case data

**Epic:** [E08 — design system conformance](../epics/E08-design-system-conformance.md)

The 360 dp hunt produced eight candidates, but their budgets were **estimated** at ~6.3 dp per
character. An estimate is a lead; a render is a measurement. This ticket makes all eight visible at
360 dp with the longest data a real user can produce, and **fixes nothing** — the author looks first
and decides which are real.

## The worst case, stated so it is not a judgement call

Realistic-worst, not absurd: person `"María Fernanda Rodríguez Quispe"`; category/account
`"Cuidado personal y salud"` and `"Tarjeta de crédito BCP"` — both user-renameable, so no ceiling;
date `"30 de septiembre de 2026"` — the long months, not `agosto`; amount `"S/ 999,999.99"`;
`"100%"`; `"999 mov."`.

## Done when

- [ ] Each candidate carries one **new** preview function named `<Subject>OverflowPreview`, holding
      only worst-case data. Every existing preview and fixture is untouched.
- [ ] Screens (`LoanDetailScreen`, `PersonLoansScreen`, `LoansScreen`) use `@PreviewRedmi15C`;
      components and atoms (`JcTopBar`, `SelectorPillsRow`, `DeleteTransactionDialog`,
      `CategoryShareRow`, `TopCategoryRow`, `CategoryRow`) use `@PreviewRedmi15CWidth`. Each new
      annotation stacks on a bare `@Preview`, never replaces one.
- [ ] `JcTopBar`'s preview shows the long title against **both** a left-only bar and a
      left-plus-two-right-icons bar — its title is centred on the full width with no guard, so the
      overlap only appears when the right icons are present.
- [ ] `config/detekt/baseline-ui-android-main.xml` is unchanged.
- [ ] `./gradlew qualityGate` and `./gradlew assembleDevDebug` pass. No behaviour changes, no fixes.
