# E14-05 — Settle the positive-money color rule

**Epic:** [E14 — Information hierarchy](../epics/E14-information-hierarchy.md)

## Done when

- [ ] One rule decides when a positive amount takes `success`: a positive account net in
      `AccountRow` and the loans total in `LoansSection` follow it, and Carlos' S/ 500.00 reads the
      same on `AccountsScreen` and one tap away on the loans screen.
- [ ] The rule lands in `DESIGN_SYSTEM.md` §1.4 as one sentence, and a test pins the chosen
      rendering (the positive-net branch is currently pinned by nothing but a preview).
- [ ] `./gradlew qualityGate --rerun-tasks` green.

## Context

E14-03 shipped the artboard faithfully, but the artboard never drew a positive account net:
`AccountRow` renders it monochrome while `LoansSection` signs and tints its positive total
`success`, and `PersonBalanceUi.totalOwedFormatted` now signs what the loans screen shows unsigned.
§1.4's stated failure mode — break the rule on one screen and the color goes ambiguous on all.
