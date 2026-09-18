# :feature:onboarding — CLAUDE.md

The manifesto: the first screen a fresh install opens on, and the same screen Profile's "Acerca de" revisits. One Compose screen in `com.emm.justchill.feature.onboarding`, no ViewModel and no state holder — the copy is static and the only decision is `isRevisit`.

`justchill.android.feature`, plus `androidx-material-icons-extended` for the two `Icons.AutoMirrored.Outlined` arrows.

## DI and navigation

- No Koin module: nothing here is injected, so the feature has no `onboardingModule` and no wiring file in `:androidApp`. Add both the day a ViewModel arrives.
- `OnboardingRoutes.kt` holds `ManifestoRoute` (`@Serializable`, carrying `isRevisit`) and `onboardingRoutes`, the registry `:androidApp`'s `RouteSerializationTest` unions.
- `onboardingEntries(bindings, onFirstLaunchSeen)` takes the first-launch write as a callback: `AppPreferences` lives in `:presentation`, which a feature module never depends on, so `:androidApp` supplies `{ appPrefs.firstLaunchSeen = true }` at the `entryProvider` call site.
- The revisit from Profile is the mirror case: `profileEntries` takes `onAboutClick: (AppNavigator) -> Unit` and `:androidApp` pushes `ManifestoRoute(isRevisit = true)`, since `:ui-android` cannot import this module either.

## Feature gotchas

- `isRevisit` flips the button, not the screen: "Empezar" with a forward arrow pops nothing and marks first launch seen; "Volver" with a back arrow only pops. Marking the preference on a revisit is harmless today and still wrong — the flag means "the manifesto was shown once".
- `nav.replaceAll(bindings.startTab)` on first launch, never `pop()`: the manifesto is the back stack root there, and popping it leaves an empty stack.
- `StartButton` is local because `:core:ui`'s `OutlinedCta` cannot draw it yet: its icon slot is `leading` only, so the forward arrow after "Empezar" has nowhere to go, and it is a bordered transparent box at `titleM` where this one fills with `surface1` at `titleL`. Widening the atom with a trailing slot, an optional fill and the type override is the fix; a second screen wanting the same shape is the trigger.

## Testing

`./gradlew :feature:onboarding:testDebugUnitTest`. No unit tests: the module holds one stateless composable with inline copy and no pure function to assert on. `ManifestoRoute`'s round trip is covered by `:androidApp`'s `RouteSerializationTest`.
