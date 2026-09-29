# :feature:report — CLAUDE.md

The month Report (ADR 015, wave 7): `ReportViewModel` with its `UiState` / `Intent` / `Effect`, the `CategoryShare` and `TrendsUiData` presentation models with their mappers, the share text formatter, `ReportScreen` and its `components/`, `ReportRoute` and `reportEntries`. `reportModule` in `di/` binds the ViewModel and nothing else; the five report use cases are bound in `:shared`'s `wiring/ReportWiring.kt`.

One package, `com.emm.justchill.feature.report`, split by source set (ADR 024 Decision 5, #491): `commonMain` holds `ReportViewModel`, its `UiState` / `Intent` / `Effect`, `reportModule` and the Compose-free files they import (`ReportTab`, `CategoryShare`, `TrendsUiData`, `ReportMappers`, `ReportShareFormatter`, `ReportCopy`, `ReportAmountFormat`); `androidMain` holds `ReportScreen`, `components/`, the `@Serializable` `ReportRoute`, `reportRoutes` and `reportEntries`. Nothing in `commonMain` imports Compose, Android, Navigation 3 or kotlinx-serialization.

`justchill.kmp.feature` and `justchill.kmp.robolectric`, plus `androidx-lifecycle-runtime-compose` and `androidx-material-icons-extended` on `androidMain`. It depends on `:core:domain`, `:core:presentation` and `:core:ui` only — `checkModuleBoundaries` holds that edge, so no other feature and no `:androidApp`.

## Screens

- The screen owns income, spend and savings rate for a month; the transaction list is another feature's number. Before adding a section, find the owner.
- `danger` on `SavingsRateBlock`'s deficit rate is deliberate: a deficit is a broken state, not an amount. The trend pill beside the rate and `ComparisonPill`'s unfavourable delta are comparisons and stay `PillTone.Neutral`.
- `compose_stability.conf` declares `com.emm.justchill.**` stable, so a `Money` or a `YearMonth` crossing from `:core:domain` still skips recomposition.

## State

- The browsed month never follows a midnight rollover: a rollover only corrects `isCurrentMonth` and the trends window (`ReportViewModelTest` pins it).
- `TodayFlow.today()` is the one way the ViewModel derives the date, and `TRENDS_WINDOW_MONTHS` is the window every trends read shares.
- Cross-feature navigation arrives as `onAddTransaction: (AppNavigator) -> Unit`, which `:androidApp` supplies; sharing goes out through `bindings.platform.onShareText`. The navigator handed over is the one the entry body remembered, so the host's push keeps nav3's per-scene lifecycle owner and the mid-transition guard with it.

## Testing

`./gradlew :feature:report:testAndroidHostTest`; `./gradlew test` does not reach it, the gate does. `MainDispatcherRule` and `FakeTodayFlow` come from `:core:testing`; the real `ClockTodayFlow` hangs `runTest`, because its self-rescheduling `delay` shares the scheduler and `advanceUntilIdle()` never returns.

The module runs three composition suites, `ReportTopBarTest`, `IncomeByCategoryBarsTest` and `ComparisonPillTest`, on `feature/loan/CLAUDE.md` "## Composition tests" (the `justchill.kmp.robolectric` opt-in and `src/androidHostTest/resources/robolectric.properties`, nothing else). `IncomeByCategoryBarsTest` renders `IncomeByCategoryBars` and pins that TalkBack announces the category name before its amount and share.

`ReportTopBarTest` renders the stateless `ReportScreen` inside `EmmTheme` and pins the share tile's edge giveback (#271): the glyph ends 28dp from the root's right edge — the `spacing.s4` (16dp) content column plus half the 24dp gap between the 44dp `TopBarTileSize` and its 20dp glyph (16 + 12). The gap is read against the measured root rather than written down as a left edge, so the pinned number is the rule and not a coordinate derived from it. `isMonthEmpty = true` only trims the composed tree; it is not what keeps the matcher unique. `ShareReportButton` passes `contentDescription = null` and renders "Compartir reporte" as `Text`, and `onNodeWithContentDescription` matches `SemanticsProperties.ContentDescription` alone, so the top-bar tile is the only match either way. The assertion reads `useUnmergedTree = true`, since the merged node is `TopBarTile`'s 48dp touch box, not the glyph; dropping the giveback, doubling it, or deriving it from `spacing.s5` each turn the suite red.

`ComparisonPillTest` renders `ComparisonPill` in `EmmTheme` and pins that `clearAndSetSemantics` gives TalkBack "Bajó S/ 660, 66%" (direction and verb), that a tiny-base percent reads "S/ 712 · más de 999%", and that `comparisonPillDescription` says the verb; `ComparisonPercentLabelTest` pins the 999 cap.
