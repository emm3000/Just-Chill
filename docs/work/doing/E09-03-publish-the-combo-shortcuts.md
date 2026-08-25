# E09-03 — Publish the combo shortcuts

The channel works (E09-02) but nothing pushes a shortcut into it. Long-pressing the launcher icon
should offer the most-used combos. `GetFrequentCombosUseCase` ranks them; `FrequentCombo` carries
ids only, so the labels need account and category names.

## Done when

- A core-side source returns shortcut-ready combos — ids, type and both labels as plain strings, no
  Android type. **Which combos to surface and what they read is core logic; only `ShortcutManagerCompat`
  and the `Intent` are Android.** It is the iOS-exported surface, so keep `java.*`/`android.*` out.
- The publisher lives where `MainActivity` is reachable, builds an explicit `Intent` for it with
  `ACTION_ADD_TRANSACTION` and the `EXTRA_*` constants, and works on the dev flavor, whose
  application id carries a suffix.
- `setDynamicShortcuts` — not `pushDynamicShortcut` — so a combo that falls out of the ranking
  disappears and one whose category was deleted is pruned on the next publish.
- **At most three combos.** Launchers show four or five slots and the static `loans` shortcut
  competes for them; ranking is most-used first.
- Short label is the category alone; long label names the account too. Spanish, `tú`, never `vos`.
- Publishing runs off the cold-start critical path; a set that lags one launch is acceptable, and a
  failure to publish is silent — no crash, no snackbar. The launcher is a convenience.
- Host tests cover the core-side mapping: fewer combos than the cap, a combo whose account or
  category no longer resolves, and the label format.
- Verified live on a device: long-press the launcher icon, see the combos, tap one, land on the form
  preselected — with `loans` still present.
- `./gradlew qualityGate && ./gradlew assembleDevDebug` green.

## Not in scope

Per-category icons, `requestPinShortcut`, the Quick Settings tile, republishing after a save.
