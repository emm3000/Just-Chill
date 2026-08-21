# E05-02 — Add the loans schema

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-01

## Done when

- [ ] `loans.sq` and `loan_payments.sq` exist under
  `data/src/commonMain/sqldelight/com/emm/data/`, with `loan_payments.loanId → loans ON DELETE
  RESTRICT` and the usual `createdAt`/`updatedAt`/`deletedAt`/`syncState` columns
- [ ] `5.sqm` adds both tables additively — no `DROP`, no column rewrite on an existing table
- [ ] `databases/6.db` is regenerated via `./gradlew :data:generateCommonMainEmmDatabaseDataSchema`
  and `./gradlew :data:verifySqlDelightMigration` passes
- [ ] `MigrationV5ToV6Test` migrates to `EmmDatabaseData.Schema.version`, never a hardcoded `6`
  ([E02](../epics/E02-migration-coverage.md) — coverage is per starting version, not per step)

## Context

Schema is at v5 today: `.sqm` files run `0`–`4`, snapshot is `databases/5.db`. `occurredAt` is ISO
local `TEXT` like `transactions.occurredAt`; `principal`/`totalDue`/payment amounts are `Money.cents`
→ `INTEGER`.
