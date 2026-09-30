# :core:presentation — CLAUDE.md

The Compose-free half of the shared UI vocabulary (ADR 024 Decision 2): the MVI base in `mvi/`, the error copy in `error/` (`DomainException.toUserMessage()`, `ValidationCode.toUserMessage()`), the Spanish money, date and search formatters in `format/`, the capture date shortcuts (`DateShortcut` with its `label` and `isActiveFor`, `dateShortcutsOf(today)`) in `date/`, and the presentation models the ViewModels build — `CategoryUi` and `SelectableCategory` with `toSelectable`, the icon catalog (`CategoryIcon` ids, Spanish names and keywords, `AppIconCatalog.search`) and `selectableColorIds` with `colorLabel` in `category/`, `TransactionUi` with its `toUi` and `Catalog` with `categoriesOf` in `transaction/`, `PersonBalanceUi` with its owed-total helpers in `loan/`. Rendering them is `:core:ui`'s job: `CategoryResolve.kt`, `TransactionRow`.

`justchill.kmp.library` (`android`, `iosArm64`, `iosSimulatorArm64`): every source is in `src/commonMain`, where `androidx.compose` does not resolve, so Compose-freedom is the module boundary. Tests are in `src/androidHostTest`. Namespace `com.emm.justchill.core.presentation`, `minSdk = 28`. It depends on `:core:domain` and nothing else (`checkModuleBoundaries`), plus `:core:testing` on `androidHostTest`; the JetBrains `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` (catalog `jetbrains-lifecycle-viewmodel`, whose Android variant resolves to `androidx.lifecycle:lifecycle-viewmodel` at the same version), `kotlinx-coroutines-core` and `kotlinx-datetime` are `commonMain` `api`, because `MviViewModel` publishes `ViewModel` and `StateFlow` and the formatters publish `kotlinx.datetime`. `:core:ui`, `:androidApp` and every feature (through `justchill.android.feature`) depend on it.

## MVI base

- `MviViewModel<S, I, E>` owns the state flow, the buffered effect channel and the two error funnels. The contract each feature owes: `.claude/rules/architecture.md` `## MVI contract`.
- `launchSafe` catches once per job; `launchSafeIn` retries a failed collector three times with a doubling delay, because a dead collector never comes back for the ViewModel's life.
- Rethrow `CancellationException` before any broad catch, or every cancelled job turns into an error effect.

## Formatters

- `NumberFormatEs` and `SpanishDateFormat` are hand-rolled to match, byte for byte, what the JVM `es` / `es-PE` formatters produce. `SpanishFormatGoldenTest` pins every string; a drift there is a UX change, not a test fix.
- Grouping is `,` and the decimal mark is `.` — es-PE, the reverse of conventional Spanish.
- `integerRounded` rounds HALF_EVEN, `decimal2` rounds HALF_UP. They are not interchangeable.
- `SpanishDateFormat` holds the only Spanish month table in the app; `MonthLabelsTest` fails the build when a second one appears.
- `NumberFormatEs.cents()` takes an absolute value, so anything that can be negative branches on the sign first (`CurrencyFormat`).
- The money entry point is `Money.format()` in `MoneyFormatter.kt`.

## Testing

`./gradlew :core:presentation:testAndroidHostTest`, which `./gradlew test` never reaches; `:core:presentation:compileKotlinIosSimulatorArm64` proves the common source compiles for iOS. Both are on the gate. Pure `kotlin.test` suites plus `MviViewModelTest`, which takes `:core:testing`'s `MainDispatcherRule`.
