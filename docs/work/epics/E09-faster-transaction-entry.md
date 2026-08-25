# E09 — Faster transaction entry

## Why

The add-transaction form is already optimised: `FrequentComboChip` sets type, category and account
in one tap, `QuickChip` fills the amount, and the ViewModel already caches the last-used account.
The remaining cost is not the form — it is *reaching* it. Every ticket here shortens the path from
"money just left my pocket" to "the amount pad is on screen", from outside the app.

## Constraints

- **The amount digits are irreducible.** Whatever the entry point, the author still types the
  number. Any candidate that claims to remove that step is either guessing the amount or reading
  someone else's notification. Optimise everything around the number, never the number.

- **Do not add to the form.** A ticket that reaches for a new field, chip row or sheet inside
  `AddTransactionScreen` is in the wrong epic. This one only builds doors.

- **`selectFrequentCombo` silently no-ops before the data lands.** It returns early when the account
  or category is not in state yet, and `accountRepository.all()` / `categoryRepository.all()` are
  flows resolved after `init`. Any preselection arriving from outside the app arrives on a cold
  start, i.e. always before them. A preselection path that fires once and forgets is a defect that
  no compiler and no gate can see — it must survive the data arriving late, and a host test must
  pin that ordering.

- **A combo can name an account or category that no longer exists.** Shortcuts outlive the rows
  they were built from: the launcher keeps a pinned icon after the category is deleted. Resolve by
  id, fall back to the normal defaults, never crash and never show an empty selection.

- **Flavor resources replace, not merge.** `androidApp/src/main/res/xml/shortcuts.xml` and
  `androidApp/src/dev/res/xml/shortcuts.xml` are two full copies of the same file, and the dev build
  reads only its own. Every shortcut change lands in both; `ShortcutXmlActionsTest` pins the action
  strings against both XMLs.

- **Which combos to surface is core logic; pushing them to the launcher is Android.** Ranking lives
  behind `GetFrequentCombosUseCase` in `:domain`. `ShortcutManagerCompat`, `TileService`, Glance and
  every other launcher API stay in `:androidApp` / `:ui-android`, per the KMP rule in `CLAUDE.md`.

- **`TileService#startActivityAndCollapse(Intent)` throws for apps targeting 34+.** We target 36.
  Use `setActivityLaunchForClick(pendingIntent)` — it also drops `onClick()` entirely. Most examples
  online still show the signature that now throws. `TileService` itself is not deprecated.

- **A Quick Settings tile nobody drags out of the drawer does not exist.**
  `StatusBarManager.requestAddTileService` (API 33+) is the only realistic adoption path, and
  `minSdk = 28` means it needs a version guard. The tile is cosmetic: below API 33, do not offer it.

- **The preselect is consumed once per ViewModel, and it has to be (E09-01).** The entry's
  `LaunchedEffect(key)` restarts whenever the composition is rebuilt while the ViewModel survives —
  rotation, theme or locale change, and pop-back from `CategoryRoute`, which this screen reaches
  through its own `onAddNewCategory`. Without `preselectConsumed` the second firing silently reverts
  whatever the user had chosen. Any future entry point that hands the form a starting state inherits
  this, not just shortcuts.

- **A shortcut navigates with `AppNavigator.pushToTop`, never `push` (E09-02).** `push` guards with
  `backStack.contains(route)` — a whole-stack scan, so it is a duplicate guard and never a move-to-top:
  once the target is buried it silently does nothing, and the user taps a launcher icon and goes
  nowhere. This regressed the shipped Préstamos shortcut once already. `pushToTop` matches by runtime
  class, pops what sits above, then reveals an equal route or replaces one carrying a stale combo.

- **A form opened with a preselection arrives with `hasChanges = true`**, because the preselected
  type goes through `changeTransactionType`, which ends in `touched()`. Nothing reads
  `AddTransactionUiState.hasChanges` today. Anyone wiring a discard-changes prompt to it must fix
  this first, or every shortcut launch will claim the user has unsaved work.

## Rejected, with the reason

- **Notification with `RemoteInput`** — wins on raw taps, loses on real cost: typing "comida" is more
  keystrokes than tapping a chip, free text needs a parser, a failed parse fails silently, and it
  drags a permanent notification plus `POST_NOTIFICATIONS` on 13+.
- **Notification listener reading Yape/BCP** — `BIND_NOTIFICATION_LISTENER_SERVICE` grants access to
  every notification on the phone behind an alarming system screen, and it is a Play-policy
  minefield for an app that promises local-only data.
