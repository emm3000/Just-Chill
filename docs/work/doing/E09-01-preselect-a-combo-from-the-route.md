# E09-01 — Preselect a combo from the route

`AddTransactionRoute` is a `data object`, so nothing outside the app can say *which* transaction it
wants. Give it the combo, and make the form open with type, account and category already set. No
launcher work here — E09-02 is what will build the shortcut that uses this.

## Done when

- `AddTransactionRoute` carries the combo as three optional fields (account id, category id,
  `TransactionType`), all defaulting to null, and every existing call site pushes `AddTransactionRoute()`.
  `CategoryRoute` is the precedent for a `@Serializable` route holding a `:domain` enum.
- `RouteSerializationTest` round-trips a populated `AddTransactionRoute`, not only the empty one.
- A new `AddTransactionIntent` carries the preselection into `AddTransactionViewModel`, and
  `transactionEntries` sends it once per route key.
- **A preselection sent before `accountRepository.all()` and `categoryRepository.all()` have emitted
  still lands.** The existing `selectFrequentCombo` returns early in that window and drops it; the new
  path holds the request until both are resolved, then clears it. This is the cold-start case, so it
  is the normal case, not the edge one.
- A preselection naming a deleted account or category leaves the ordinary defaults in place, clears
  itself, and does not crash.
- The preselected type also drives the category list and the frequent-combo row, exactly as tapping
  the type toggle does.
- `AddTransactionViewModelTest` pins three orderings: preselect-then-data, data-then-preselect, and
  preselect-with-a-missing-id.
- `./gradlew qualityGate && ./gradlew assembleDevDebug` green.

## Not in scope

`ShortcutManagerCompat`, any `shortcuts.xml` change, any new UI. The route gains a capability; nobody
calls it with a value yet except the tests.
