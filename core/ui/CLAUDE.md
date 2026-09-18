# :core:ui — CLAUDE.md

The UI vocabulary every feature shares (ADR 015): the MVI base in `mvi/`, the Spanish money, date and search formatters in `format/`, the pending recurring movement UI in `pending/`, and the design system — the tokens in `theme/`, the atoms in `atoms/`, the legacy `Emm*` widgets in `components/`, `Numpad.kt` and the `@Preview` device wrappers in `preview/`, plus the `PersonBalanceUi` model and its owed-total helpers in `loan/`, so the accounts screen reads Loan balances without the Loans feature. It depends on `:core:domain` and nothing else — `checkModuleBoundaries` holds that edge — so Koin, SQLDelight, Supabase and Ktor cannot appear here.

`justchill.android.compose` (`com.android.library` plus the Compose compiler), namespace `com.emm.justchill.core.ui`, `minSdk = 28`, one `src/main` and one `src/test`. Everything it exposes is `api`: `MviViewModel` publishes `ViewModel`, `StateFlow` and `DomainException`, the formatters publish `Money`, `YearMonth` and `kotlinx.datetime`, and the atoms publish `ImageVector`. Compose itself comes from `justchill.android.compose`, so no Compose coordinate is written here.

## Design system

- Which atom and which token, and why: `.claude/rules/ui-components.md`. That file is the style guide; the values live in `theme/EmmColors.kt`, `EmmType.kt`, `EmmSpacing.kt` and `EmmRadii.kt`.
- The Inter and IBM Plex Mono faces are this module's own resources under `src/main/res/font/`, reached through `com.emm.justchill.core.ui.R`. `EmmType` is the only file that touches `R`; `:ui-android`'s `com.emm.justchill.shared.R` no longer carries a font.
- `compose_stability.conf` declares `com.emm.justchill.**` stable, so an atom taking a `:core:domain` `Money` still skips recomposition.
- `components/` holds the legacy `EmmButton`, `EmmCard`, `EmmListItem`, `EmmTextInput` widgets. They are not the design system and no new screen reaches for them.

## Pending recurring movements

`pending/` holds `PendingRecurringUi` with its mapper, the list header and row, and `ConfirmRecurringSheet`. It lives here because the Transaction list is what shows a Pending, so neither side may depend on the Recurring feature (ADR 015, wave 5).

- The sheet never dismisses itself: confirming and skipping are callbacks the hosting list's ViewModel handles, and the caller closes the sheet once the operation succeeds.
- `PendingRecurringRow`'s `IconTileSize.Lg` matches `TransactionRow`'s tile, so a pending row's text starts on the same column as a transaction's. Shrinking it breaks the alignment no test pins.
- `danger` on an overdue period label is deliberate: a catch-up month is a broken state, not an amount.

## MVI base

`MviViewModel<S, I, E>` owns the state flow, the buffered effect channel and the two error funnels. `launchSafe` catches once per job; `launchSafeIn` retries a failed collector three times with a doubling delay, because a dead collector never comes back for the ViewModel's life. Rethrow `CancellationException` before any broad catch or every cancelled job turns into an error effect. The contract each feature owes: `.claude/rules/architecture.md` `## MVI contract`.

## Formatters

- `NumberFormatEs` and `SpanishDateFormat` are hand-rolled to match, byte for byte, what the JVM `es` / `es-PE` formatters produce. `SpanishFormatGoldenTest` pins every string; a drift there is a UX change, not a test fix.
- Grouping is `,` and the decimal mark is `.` — es-PE, the reverse of conventional Spanish.
- `integerRounded` rounds HALF_EVEN, `decimal2` rounds HALF_UP. They are not interchangeable.
- `SpanishDateFormat` holds the only Spanish month table in the app; `MonthLabelsTest` fails the build when a second one appears.
- `NumberFormatEs.cents()` takes an absolute value, so anything that can be negative branches on the sign first (`CurrencyFormat`).

## Testing

`./gradlew :core:ui:testDebugUnitTest`. Pure `kotlin.test` suites, no MockK and no dispatcher rule beyond `MviViewModelTest`'s own `Dispatchers.setMain`.
