# :core:ui — CLAUDE.md

The UI vocabulary every feature shares (ADR 015): the MVI base in `mvi/`, the navigation vocabulary in `navigation/` — `AppRoute` and `CaptureRoute : AppRoute` as plain interfaces, `AppNavigator`, `rememberAppNavigator`, `NavHostBindings` and the `PlatformHostActions` interface — the error copy in `error/` (`DomainException.toUserMessage()`, `ValidationCode.toUserMessage()`), the Spanish money, date and search formatters in `format/`, the pending recurring movement UI in `pending/`, the `PersonBalanceUi` model and its owed-total helpers in `loan/` so the accounts screen reads Loan balances without the Loans feature, the design system — the tokens in `theme/`, the atoms in `atoms/` (`FormSection` among them), the legacy `Emm*` widgets in `components/`, `Numpad.kt` and the `@Preview` device wrappers in `preview/` — and the pieces more than one feature captures a movement with: the icon and colour catalog with the selectable category in `category/`, the transaction row and its catalog in `transaction/`, and the account, category and date pickers and `AmountInputSheet` in `sheets/`. It depends on `:core:domain` and nothing else — `checkModuleBoundaries` holds that edge — so Koin, SQLDelight, Supabase and Ktor cannot appear here (`:core:testing`'s `MainDispatcherRule` is `testImplementation` only, for `MviViewModelTest`). `:core:ui` names no feature's route: `AppNavigator` holds a back stack and nothing else, the host supplies every concrete route the entries navigate to, and `popToCapture` keys on `CaptureRoute` instead of a named transaction route.

`justchill.android.compose` (`com.android.library` plus the Compose compiler), namespace `com.emm.justchill.core.ui`, `minSdk = 28`, one `src/main` and one `src/test`. Everything it exposes is `api`: `MviViewModel` publishes `ViewModel`, `StateFlow` and `DomainException`, the formatters publish `Money`, `YearMonth` and `kotlinx.datetime`, and the atoms publish `ImageVector`. Compose itself comes from `justchill.android.compose`, so no Compose coordinate is written here.

## Design system

- Which atom and which token, and why: `.claude/rules/ui-components.md`. That file is the style guide; the values live in `theme/EmmColors.kt`, `EmmType.kt`, `EmmSpacing.kt` and `EmmRadii.kt`. `theme/EdgeGiveback.kt`'s `EmmSpacing.edgeGiveback(artwork)` is the one derivation of `(s12 - artwork) / 2`, the padding a 48dp touch target hands back at a screen edge so its smaller glyph still lands on the rows' column; a feature calls it with the artwork it paints (`EmmRowMenuGlyphSize` for `EmmRowMenu`) instead of writing the difference down (#271).
- The Inter and IBM Plex Mono faces are this module's own resources under `src/main/res/font/`, reached through `com.emm.justchill.core.ui.R`. `EmmType` is the only file that touches `R`.
- `compose_stability.conf` declares `com.emm.justchill.**` stable, so an atom taking a `:core:domain` `Money` still skips recomposition.
- A new or rebuilt dialog is an `EmmDialog`, never a hand-rolled `Dialog` or an `AlertDialog`; recurring, account and transaction still carry raw ones and are the queue, not the precedent. A write in flight passes `inFlightDialogProperties(isInFlight)` and `actionsEnabled = false` together, so the scrim, the back press and both actions stop as one.
- `OutlinedCta` takes a `leading` and a `trailing` slot, both defaulted to none; `Loading` replaces `leading` with the spinner and drops `trailing`. `CtaHeight` and `titleM` are fixed for every CTA: a caller that wants a taller button or a bigger label is asking for a second CTA identity, which ADR 017 refuses.
- Every file in `atoms/` that defines a public composable carries a `@Preview` wrapped in `EmmTheme` (#185). The five without one — `AmountTone.kt`, `CtaTone.kt`, `IconBtnTone.kt`, `PillTone.kt` and `FilteredTextCursor.kt` — declare a tone enum or a `TextFieldValue` helper and no composable at all; a new atom file is expected to ship its preview with it.
- `components/` holds the legacy `EmmButton`, `EmmCard`, `EmmListItem`, `EmmTextInput` widgets. They are not the design system and no new screen reaches for them.

## Pending recurring movements

`pending/` holds `PendingRecurringUi` with its mapper, the list header and row, and `ConfirmRecurringSheet`. It lives here because the Transaction list is what shows a Pending, so neither side may depend on the Recurring feature (ADR 015, wave 5).

- The sheet never dismisses itself: confirming and skipping are callbacks the hosting list's ViewModel handles, and the caller closes the sheet once the operation succeeds.
- `PendingRecurringRow`'s `IconTileSize.Lg` matches `TransactionRow`'s tile, so a pending row's text starts on the same column as a transaction's. Shrinking it breaks the alignment no test pins.
- `danger` on an overdue period label is deliberate: a catch-up month is a broken state, not an amount.

## Capture vocabulary

- `category/` holds the two catalogs the domain's semantic ids resolve against — `AppIconCatalog` (`IconsAll.kt`) and `allColors` (`ColorsAll.kt`) — plus `CategoryUi`, `SelectableCategory` and the `resolvedIcon` / `resolvedColor` getters in `CategoryResolve.kt` that turn an `iconId` / `colorId` into an `ImageVector` and a `CategoryColor`. A stored `ImageVector` or `Color` is the bug this package exists to prevent.
- `transaction/` holds `TransactionUi` with its `toUi` mapper, the `Catalog` a capture form cuts its lists out of, and `TransactionRow`. `Catalog.Loading` is not an empty `Loaded`; only `loaded` tells the two apart.
- `sheets/` holds `AccountPickerSheet`, `CategoryPickerSheet` and `DatePickerSheet`, shared by the transaction, recurring and loan forms. A sheet used by one feature stays in that feature's own module — `NoteSheet` is the example.
- `SelectorChip`, `FrequentComboChip` and the `CategoryDot` they draw are atoms like any other, under `atoms/`.

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
