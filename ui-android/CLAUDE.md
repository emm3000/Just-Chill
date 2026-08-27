# :ui-android — CLAUDE.md

Android's Compose UI module: screens, navigation, theme and widgets. It renders what
`:presentation` exposes and owns nothing else — no ViewModels, no DI, no formatters.

**Android-only** (ADR 011 dropped the iOS target).

Root package `com.emm.justchill.{hh.<feature>, core, components}`. `minSdk = 28`. Depends on
`:presentation` (api), `:domain`, `:data`.

## Where things live

Plain `com.android.library` (ADR 011/E11-03) — one `src/main`, one `src/test`, no KMP plugin, no
`commonMain` to keep code out of. Compose comes from Google's own BOM-managed artifacts, not the
JetBrains Compose Multiplatform ports — those would have pulled in a material3 alpha.

A feature owns `hh/<feature>/` — its Screens plus `<Feature>Entries.kt` for nav wiring.
Cross-feature: `hh/shared/` (nav host, bottom bar, atoms), `core/theme/`, `components/`.

## DI

No modules here, and no exception. `appModules(platformModule)` / `bootstrapAppGraph` live in
`:presentation` (`core/AppGraph.kt`), the Android platform module lives in `:androidApp`, and a new
feature registers its Koin module in `:presentation`'s `appModules()`.

## No expect/actual

There is none — one target, one source set. `hh/shared/PlatformHostActions.kt`'s capability flags
(`supportsBackup`, `showGoogleSignIn`, …) are plain constants; the seam survives because the nav
entries read it, and collapsing that is a separate change.

## Navigation

`AppNavHost` runs Google's navigation3, runtime **and** UI. `rememberNavBackStack(vararg NavKey)`
stores each entry as its class name and re-resolves it through
`Class.forName(name).kotlin.serializer()`, so there is no subtype registry to keep in sync.

**Landmine:** every route the host can push MUST be `@Serializable`, and so must its fields. An
unserializable route crashes `rememberNavBackStack` on process-death restore and nowhere else —
invisible to the compiler and to the build gate. `RouteSerializationTest` reflects over sealed
`AppRoute` and round-trips each route through that exact serializer pair to catch it.

**Landmine (a door nobody can open):** a feature's only entry point must never be conditional on
that feature already having data. `LoansCard` was the sole push of `LoansRoute` and rendered behind
`hasLoans`, which needs a loan, which needs the screen the card opens — the feature was unreachable
and `LoansScreen`'s empty state was code no user could ever see (`15b8783a`). Nothing catches this:
the push exists, so a route-graph check passes, and this module has no UI test harness. Gate the
*content* of an entry point, never its existence.

**Landmine (nav3 entry caching):** `NavEntry.content` closures are cached until the back stack
changes. Host state an entry reads must arrive as `() -> T` accessors, never by value — see the
result channels in `AppNavHost.kt` (`pendingCategory`, `pendingImportJson`).

## MVI (consumed, not owned)

ViewModels extend `MviViewModel<S, I, E>` from `:presentation` (`core/mvi/`). Screens are
callback-driven composables taking `state` + `onIntent`; `SnackbarHostState` lives in the root
`Scaffold` of `AppNavHost.kt`. State classes carry semantic ids (`iconId`/`colorId`) — resolve
them at render time via `CategoryResolve.kt`, never in a mapper.

**Stability:** `:presentation`'s state classes are external to this module's compose compilation
and carry no `@Stable`/`@Immutable`. `compose_stability.conf` (wired via
`stabilityConfigurationFiles`) declares them — and `:domain` values — stable. If a list screen
ever stutters, check compose compiler metrics before blaming the pattern.

## UI conventions

- Google's Compose, every artifact governed by `androidx-compose-bom` — **never add a `version.ref`
  to a Compose library**, the BOM decides. Tokens in `core/theme/` are the source of truth.
- Resources are ordinary Android resources under `src/main/res/` (`font/`, `drawable/`), reached
  through `com.emm.justchill.shared.R` — not `composeResources` / `Res.*`.
- `koin-compose` still pulls a handful of `org.jetbrains.compose.*` artifacts. They are shims whose
  only dependency is the matching `androidx.compose.*` one, so nothing extra ships; swapping to
  `koin-androidx-compose` would remove them and touch every injection site.
- User-facing strings are **Spanish** (from `:presentation`'s `UiStrings`); code, comments and
  identifiers are **English**.
- Errors reach the user through `DomainException.toUserMessage()` (`:presentation` `core/error/`),
  never a raw exception message.
- A screen never applies window insets itself — `AppNavHost`'s `Scaffold` owns them via
  `contentWindowInsets = WindowInsets.safeDrawing` and hands them down as the content
  `PaddingValues`; adding `statusBarsPadding`/`navigationBarsPadding`/`imePadding` double-pads.
  Exception: `ModalBottomSheet` is its own window and sets its own `contentWindowInsets`.

## Testing

`./gradlew :ui-android:testDebugUnitTest`. Pure UI logic gets a plain function next to the screen
and a test here — that is what `commitHashUi()` is. The Koin graph test and the formatter/mapper
suites belong to `:presentation`.
