# E09-02 — Carry a combo from the launcher

E09-01 gave `AddTransactionRoute` the combo fields; nothing fills them yet. Build the channel: a
launcher intent carrying account, category and type opens the form preselected. Pushing the
shortcuts is E09-03 — this one is provable with `adb shell am start`.

## Done when

- A new action constant and the three extra keys sit beside `ACTION_OPEN_LOANS` in
  `ShortcutRoutes.kt`; `:androidApp` reads them through those constants, never a repeated literal.
- `MainActivity` forwards the extras as plain `String`s. **`NavKey`, `AppRoute` and `:domain` types
  must not appear in `:androidApp`** — nav3 is `implementation`, not `api`, on purpose, and E06-11
  already died on this. Carry them in one holder declared in `:ui-android` rather than growing
  `AppNavHost`'s parameter list.
- `shortcutRouteToPush` maps the new action plus its extras to a populated `AddTransactionRoute`,
  and still maps `ACTION_OPEN_LOANS` and an unknown action exactly as it does today.
- **The extras are untrusted input** — `MainActivity` is exported. An unparseable or absent type, an
  empty id, a missing extra: each degrades to a plain add-transaction screen, nothing throws.
- The `savedInstanceState == null` guard and the `shortcutRequestId` counter keep working, and
  firing the same combo twice does not stack two entries — `AppNavHost` uses `backStack::add`, which
  bypasses `AppNavigator.push`'s by-value guard. See the epic.
- `ShortcutRoutesTest` covers each extra absent, a garbage type, an empty id, loans unchanged, and
  the full combo.
- Verified live: `adb shell am start -a <action> --es <keys> …` on a dev build opens the form with
  the combo selected, and back returns to the start tab rather than leaving the app.
- `./gradlew qualityGate && ./gradlew assembleDevDebug` green.

## Not in scope

`ShortcutManagerCompat`, `shortcuts.xml`, any pin request, any new UI.
