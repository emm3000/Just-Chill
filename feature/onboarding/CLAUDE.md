# :feature:onboarding — CLAUDE.md

The manifesto: the first screen a fresh install opens on, and the same screen Profile's "Acerca de" revisits. One Compose screen in `com.emm.justchill.feature.onboarding`, no ViewModel and no state holder — the copy is static and the only decision is `isRevisit`.

`justchill.android.feature`, plus `androidx-material-icons-extended` for the two `Icons.AutoMirrored.Outlined` arrows.

## DI and navigation

- No Koin module: nothing here is injected, so the feature has no `onboardingModule` and no wiring file in `:androidApp`. Add both the day a ViewModel arrives.
- `OnboardingRoutes.kt` holds `ManifestoRoute` (`@Serializable`, carrying `isRevisit`) and `onboardingRoutes`, the registry `:androidApp`'s `RouteSerializationTest` unions.
- `onboardingEntries(bindings, onFirstLaunchSeen)` takes the first-launch write as a callback: `AppPreferences` lives in `:androidApp`'s `core/preferences/`, which a feature module never depends on, so `:androidApp` supplies `{ appPrefs.firstLaunchSeen = true }` at the `entryProvider` call site.
- The revisit from Profile is the mirror case: `profileEntries` takes `onAboutClick: (AppNavigator) -> Unit` and `:androidApp` pushes `ManifestoRoute(isRevisit = true)`, since one feature never imports another.

## Feature gotchas

- `isRevisit` flips the button, not the screen: "Empezar" with a forward arrow pops nothing and marks first launch seen; "Volver" with a back arrow only pops. Marking the preference on a revisit is harmless today and still wrong — the flag means "the manifesto was shown once".
- `nav.replaceAll(home)` on first launch, never `pop()`: the manifesto is the back stack root there, and popping it leaves an empty stack. `home` is `:androidApp`'s `HOME_ROUTE`, the amount pad, supplied at the `entryProvider` call site for the same reason `onFirstLaunchSeen` is — a feature module never names another feature's route.
- The CTA is `:core:ui`'s `OutlinedCta`, widened with a `trailing` slot for the forward arrow (#270). The iron rule, not a second caller, is what moved it: a screen never hand-draws an atom that exists. The atom's `CtaHeight` (52dp) and `titleM` label are the whole CTA system's, so the manifesto button reads like every other one; ADR 017 is why the old `surface1` fill and the `titleL` override did not follow it into the atom.

## Testing

`./gradlew :feature:onboarding:testDebugUnitTest`. Composition tests only — there is no pure function and no ViewModel here, so the harness `:feature:loan` piloted is what reaches the one decision: Robolectric and `androidx.compose.ui:ui-test-junit4` on `testImplementation`, `ui-test-manifest` on `debugImplementation`, `testOptions.unitTests.isIncludeAndroidResources` in this build file, and `src/test/resources/robolectric.properties` carrying `sdk=35` and `qualifiers=w411dp-h891dp` once for the module rather than a `@Config` per suite.

`ManifestoScreenTest` renders `ManifestoScreen` inside `EmmTheme` and pins the `isRevisit` branch by label: "Empezar" on a fresh install, "Volver" on a revisit, each with a click action and each invoking `onStart` exactly once, plus the absence of "Empezar" on the revisit. The arrow is decorative and hidden — `ManifestoArrow` passes `contentDescription = null`, so its side never reaches the semantics tree and the label alone names the action. Both branches also assert `Role.Button`, which the atom carries and the old hand-drawn `Box` did not.

`OnboardingEntriesTest` reaches `onboardingEntries` without a `NavDisplay` — nav3's `entryProvider { }` returns a `(NavKey) -> NavEntry<NavKey>` and `NavEntry.Content()` is public, so the real entry body composes against a real `NavBackStack` and a test `AppRoute` standing in for the home pad. First launch calls `onFirstLaunchSeen` once and leaves the stack rooted at that route; the revisit pops one entry and never writes the preference. The revisit seeds three entries on purpose: `AppNavigator.pop()` refuses a stack of one, and on a two-entry stack `pop()` and `replaceAll(home)` both land on `[home]`, so the stack shape alone would not tell the branches apart. Composing an entry outside a `NavDisplay` also makes `LocalLifecycleOwner` the Robolectric activity, always RESUMED, so `AppNavigator`'s mid-transition guard is inert here — the full recipe and its caveat live in `feature/loan/CLAUDE.md`.

`ManifestoRoute`'s round trip is covered by `:androidApp`'s `RouteSerializationTest`.
