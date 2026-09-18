# :feature:report — CLAUDE.md

The month Report (ADR 015, wave 7): `ReportViewModel` with its `UiState` / `Intent` / `Effect`, the `CategoryShare` and `TrendsUiData` presentation models with their mappers, the share text formatter, `ReportScreen` and its `components/`, `ReportRoute` and `reportEntries`. `reportModule` in `di/` binds the ViewModel and nothing else; the five report use cases are bound in `:androidApp`'s `wiring/ReportWiring.kt`.

`justchill.android.feature`, namespace `com.emm.justchill.feature.report`, `minSdk = 28`, one `src/main` and one `src/test`. It depends on `:core:domain` and `:core:ui` only — `checkModuleBoundaries` holds that edge, so no other feature and no `:androidApp`.

## Screens

- The screen owns income, spend and savings rate for a month; the transaction list is another feature's number. Before adding a section, find the owner.
- `danger` on `SavingsRateBlock`'s deficit rate is deliberate: a deficit is a broken state, not an amount. The trend pill beside the rate is a comparison and stays `PillTone.Neutral`.
- `ReportFormat.kt` maps the domain's colour *name* to a `:core:ui` token, so the palette can be retuned without a migration.
- `compose_stability.conf` declares `com.emm.justchill.**` stable, so a `Money` or a `YearMonth` crossing from `:core:domain` still skips recomposition.

## State

- The browsed month never follows a midnight rollover: a rollover only corrects `isCurrentMonth` and the trends window (`ReportViewModelTest` pins it).
- `TodayFlow.today()` is the one way the ViewModel derives the date, and `TRENDS_WINDOW_MONTHS` is the window every trends read shares.
- Cross-feature navigation arrives as `onAddTransaction: (AppNavigator) -> Unit`, which `:androidApp` supplies; sharing goes out through `bindings.platform.onShareText`. The navigator handed over is the one the entry body remembered, so the host's push keeps nav3's per-scene lifecycle owner and the mid-transition guard with it.

## Testing

`./gradlew :feature:report:testDebugUnitTest`. `MainDispatcherRule` and `FakeTodayFlow` come from `:core:testing`; the real `ClockTodayFlow` hangs `runTest`, because its self-rescheduling `delay` shares the scheduler and `advanceUntilIdle()` never returns.
