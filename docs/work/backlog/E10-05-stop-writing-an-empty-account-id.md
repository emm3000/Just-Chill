# E10-05 — Stop writing an empty account id on a recurring template

**Epic:** [E10 — ViewModel state hygiene](../epics/E10-viewmodel-state-hygiene.md)

## Done when

- [ ] `rg 'AccountId\(.*orEmpty\(\)\)' --type kotlin` returns nothing
- [ ] a template cannot be saved without a resolved account — the type says so, or the save path
      refuses it, rather than `isSaveEnabled` being the only thing in the way
- [ ] a test drives the save with `selectedAccount` unresolved and asserts no write reaches the
      repository
- [ ] `./gradlew qualityGate --rerun-tasks` is green

## Context

`AddEditRecurringMovementViewModel` builds its insert with
`accountId = AccountId(selectedAccount?.accountId?.value.orEmpty())`. An unresolved selection writes
`AccountId("")` — a row pointing at no account, matching nothing, reported to the user as saved.
It is the same silent no-op shape E10-04 removed from the edit form's delete path, and today only
the CTA's `isSaveEnabled` holds it shut. The sibling line above it already writes the resolved
category, so the shape is inconsistent within one function.
