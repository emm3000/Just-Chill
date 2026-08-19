# :ui-android — CLAUDE.md

Android's Compose UI module: screens, navigation, theme and widgets. It renders what
`:presentation` exposes and owns nothing else — no ViewModels, no DI, no formatters.

**Android-only** (`justchill.kmp.ios=false` in this module's gradle.properties — ADR 005): the iOS
app is native SwiftUI over `:presentation`'s JustChillKit framework.

Root package: `com.emm.justchill.{hh.<feature>, core, components}` — the SAME packages as
`:presentation` (the extraction never renamed packages), so same-package symbols across the module
boundary need explicit imports. `minSdk = 28`. Depends on `:presentation` (api), `:domain`, `:data`.

This is where Android feature UI happens. `:androidApp` is a thin shell around it.

## Where things live

Everything is in `androidMain` — there is no `commonMain`. The UI sat in `commonMain` until the
module went Android-only, on the theory that a second Compose target might arrive; ADR 005 sent iOS
to SwiftUI and no other target ever claimed it. A KMP `commonMain` can only resolve artifacts that
publish multiplatform metadata, so keeping it there forced the JetBrains Compose Multiplatform
ports — and with them a material3 **alpha** in the production UI. `androidMain` takes Google's own
BOM-managed artifacts instead. **Do not move code back to `commonMain`**: it silently drags the CMP
ports back in.

A feature owns `hh/<feature>/` — its Screens plus `<Feature>Entries.kt` for nav wiring.
Cross-feature: `hh/shared/` (nav host, bottom bar, atoms), `core/theme/`, `components/`.

## DI

No modules here. `appModules(platformModule)` / `bootstrapAppGraph` live in `:presentation`
(`core/AppGraph.kt`); the Android platform module lives in `:androidApp`.

No exception either: the commit-hash contract used to live here as a `COMMIT_HASH_QUALIFIER` string
and is now `:presentation`'s `core/CommitHash.kt`, bound and resolved by type. `AppNavHost` unwraps
it at the injection point; `hh/profile/CommitHashUi.kt` keeps only the footer's presentation state.

A new feature registers
its Koin module in `:presentation`'s `appModules()`, never here — and its ViewModel goes into
`AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS` (now in `:presentation`'s androidHostTest).

## No expect/actual

There is none — one target, one source set. `hh/shared/PlatformHostActions.kt` was the last pair and
collapsed into a single declaration when the sources moved. Its capability flags
(`supportsBackup`, `showGoogleSignIn`, …) are now constants; the seam survives because the nav
entries read it, and collapsing that is a separate change.

## Navigation

`AppNavHost` on Google's navigation3, runtime **and** UI. Slice F had put the UI on the JetBrains
CMP port so one host could drive Android and iOS; iOS left for SwiftUI, so the port went too — and
so did slice F's explicit `SavedStateConfiguration`, which only ever existed because Kotlin/Native
has no reflective serializer discovery. The host now calls the Android-only
`rememberNavBackStack(vararg NavKey)` with a single route: `NavKeySerializer` stores each entry as
its class name and re-resolves it with `Class.forName(name).kotlin.serializer()`, so there is no
subtype registry to keep in sync.

**Landmine:** every route the host can push MUST be `@Serializable` (and so must its fields). An
unserializable route crashes `rememberNavBackStack` on process-death restore and nowhere else —
invisible to the compiler and the build gate. `RouteSerializationTest` reflects over sealed
`AppRoute` and round-trips each route through that exact serializer pair to catch it.

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
  to a Compose library**, the BOM decides. material3 resolves stable (`1.4.0` at the time of the
  move), not the alpha the CMP port pinned. Tokens in `core/theme/` are the source of truth.
- Resources are ordinary Android resources under `androidMain/res/` (`font/`, `drawable/`), reached
  through `com.emm.justchill.shared.R` — not `composeResources` / `Res.*`.
- `koin-compose` still pulls a handful of `org.jetbrains.compose.*` artifacts. They are shims whose
  only dependency is the matching `androidx.compose.*` one, so nothing extra ships; swapping to
  `koin-androidx-compose` would remove them and touches 14 files.
- User-facing strings are **Spanish** (from `:presentation`'s `UiStrings`); code, comments and
  identifiers are **English**.
- Errors reach the user through `DomainException.toUserMessage()` (`:presentation` `core/error/`),
  never a raw exception message.

## Testing

- `./gradlew :ui-android:testAndroidHostTest` — JVM host tests. `--rerun` is a **per-task** option:
  with several tasks in one invocation it forces only the task it follows.
- Lives here: `AppNavigatorTest`, `RouteSerializationTest`, `HighlightQuotedTest`,
  `CommitHashUiTest`. The Koin graph test and the formatter/mapper suites belong to
  `:presentation`. Pure UI logic gets a plain function next to the screen and a test here — that
  is what `commitHashUi()` is. Its sentinel still has a second copy in `build-logic`
  (`GenerateBuildInfoTask.UNKNOWN_COMMIT`), which cannot be on the app's compile classpath;
  `CommitHashUiTest` spells that word out so retyping `UNKNOWN_COMMIT_HASH` goes red, and
  `GenerateBuildInfoTaskTest.the sentinel is the exact word the app side spells out` pins the
  `build-logic` side, so retyping `UNKNOWN_COMMIT` goes red too.

## Gate

On the standard `qualityGate` (detekt + host tests + dev lint); the iOS compile leg runs through
`:domain`/`:data`/`:presentation`, not here. Never gate on plain `./gradlew detekt`: it is
`NO-SOURCE` on every KMP module.
