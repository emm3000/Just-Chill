# :core:ui — CLAUDE.md

The UI vocabulary every feature shares (ADR 015): the MVI base in `mvi/` and the Spanish money, date and search formatters in `format/`. It depends on `:core:domain` and nothing else — `checkModuleBoundaries` holds that edge — so Koin, SQLDelight, Supabase and Ktor cannot appear here. The theme and the atoms land in a later ADR 015 ticket.

`justchill.android.compose` (`com.android.library` plus the Compose compiler), namespace `com.emm.justchill.core.ui`, `minSdk = 28`, one `src/main` and one `src/test`. Everything it exposes is `api`: `MviViewModel` publishes `ViewModel`, `StateFlow` and `DomainException`, and the formatters publish `Money`, `YearMonth` and `kotlinx.datetime`.

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
