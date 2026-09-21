# :feature:report — CLAUDE.md

The month Report (ADR 015, wave 7): `ReportViewModel` with its `UiState` / `Intent` / `Effect`, the `CategoryShare` and `TrendsUiData` presentation models with their mappers, the share text formatter, `ReportScreen` and its `components/`, `ReportRoute` and `reportEntries`. `reportModule` in `di/` binds the ViewModel and nothing else; the five report use cases are bound in `:androidApp`'s `wiring/ReportWiring.kt`.

`justchill.android.feature`, namespace `com.emm.justchill.feature.report`, `minSdk = 28`, one `src/main` and one `src/test`. It depends on `:core:domain` and `:core:ui` only — `checkModuleBoundaries` holds that edge, so no other feature and no `:androidApp`.

## Screens

- The screen owns income, spend and savings rate for a month; the transaction list is another feature's number. Before adding a section, find the owner.
- `danger` on `SavingsRateBlock`'s deficit rate is deliberate: a deficit is a broken state, not an amount. The trend pill beside the rate is a comparison and stays `PillTone.Neutral`.
- `compose_stability.conf` declares `com.emm.justchill.**` stable, so a `Money` or a `YearMonth` crossing from `:core:domain` still skips recomposition.

## State

- The browsed month never follows a midnight rollover: a rollover only corrects `isCurrentMonth` and the trends window (`ReportViewModelTest` pins it).
- `TodayFlow.today()` is the one way the ViewModel derives the date, and `TRENDS_WINDOW_MONTHS` is the window every trends read shares.
- Cross-feature navigation arrives as `onAddTransaction: (AppNavigator) -> Unit`, which `:androidApp` supplies; sharing goes out through `bindings.platform.onShareText`. The navigator handed over is the one the entry body remembered, so the host's push keeps nav3's per-scene lifecycle owner and the mid-transition guard with it.

## Testing

`./gradlew :feature:report:testDebugUnitTest`. `MainDispatcherRule` and `FakeTodayFlow` come from `:core:testing`; the real `ClockTodayFlow` hangs `runTest`, because its self-rescheduling `delay` shares the scheduler and `advanceUntilIdle()` never returns.

`ReportTopBarTest` is this module's composition-test suite, copied from `feature/loan/CLAUDE.md` "## Composition tests" (the three dependency lines, `robolectric.properties`, nothing else). It renders the stateless `ReportScreen` inside `EmmTheme` and pins the share tile's edge giveback (#271): the glyph ends 28dp from the root's right edge — the `spacing.s4` (16dp) content column plus half the 24dp gap between the 44dp `TopBarTileSize` and its 20dp glyph (16 + 12). The gap is read against the measured root rather than written down as a left edge, so the pinned number is the rule and not a coordinate derived from it. `isMonthEmpty = true` only trims the composed tree; it is not what keeps the matcher unique. `ShareReportButton` passes `contentDescription = null` and renders "Compartir reporte" as `Text`, and `onNodeWithContentDescription` matches `SemanticsProperties.ContentDescription` alone, so the top-bar tile is the only match either way. The assertion reads `useUnmergedTree = true`, since the merged node is `TopBarTile`'s 48dp touch box, not the glyph; dropping the giveback, doubling it, or deriving it from `spacing.s5` each turn the suite red.
