# E01-04 — Automated RLS probe for the backups bucket

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] two authenticated users each prove they can reach only their own prefix in the `backups` bucket
- [ ] neither can list, read, or delete the other's objects
- [ ] the probe runs automatically, not only by hand against a local stack

## Context

Today's only proof is a manual probe run once against a local stack — `qualityGate` cannot see a
SQL policy and the repo has no Supabase test infrastructure.
