# :feature:auth — CLAUDE.md

Sign-in and sign-up for the snapshot-backup account (ADR 009): the email/password form, the Google button, and the "revisa tu correo" step after a sign-up that needs confirmation. ViewModel, state and Compose screen together in `com.emm.justchill.feature.auth`.

`justchill.android.feature`, which brings `:core:domain`, `:core:ui`, Koin, lifecycle, navigation3-runtime, the serialization plugin and `:core:testing`. The build file adds two libraries of its own: `androidx-activity-compose` for the `BackHandler` that keeps the CheckEmail step from popping the back stack, and `androidx-material-icons-extended` for the mail and password-visibility glyphs.

## DI and navigation

- `authModule` exposes `AuthViewModel` and nothing else. `:androidApp`'s `wiring/AuthWiring.kt` `includes` it and binds `AuthRepository` plus the seven auth use cases, because `DefaultAuthRepository` lives in `:core:backup` and a use case is not the feature's to own. The `SignOut`, `GetSessionStatus` and `DeleteUserAccount` bindings are there for Profile, not for this module.
- `AuthRoutes.kt` holds `AuthRoute`, `@Serializable`, plus `authRoutes`, the registry `:androidApp`'s `RouteSerializationTest` unions. A new route lands in both or it is never round-tripped.
- `authEntries` takes only `NavHostBindings`: nothing navigates out of auth except `nav.pop()`. The door in is Profile's `onSignInClick: (AppNavigator) -> Unit`, supplied from `AppNavHost`, since one feature never imports another.

## Feature gotchas

- `GoogleSignInLauncher` and `GoogleSignInResult` are this module's contract; the Activity-backed `ActivityGoogleSignInLauncher` and the Credential Manager client stay in `:androidApp` (`core/auth/`), bound by `androidPlatformModule` (#105 story 34). The dependency runs app-to-feature and never back.
- `googleServerClientId` arrives as a `named("googleServerClientId")` string from `BuildConfig`. Blank when `supabase.properties` is absent: `submitWithGoogle` short-circuits and `showGoogleSignIn` hides the button, so the launcher is never reached.
- `ic_google.xml` is drawn with `Image`, not `Icon`: Google's branding requires the multicolor G untinted.
- The resend link has two flags. `isResending` covers the network call only; `canResend` covers the 30-second cooldown that follows a success. A failed resend leaves `canResend` true.

## Testing

`./gradlew :feature:auth:testDebugUnitTest`. `AuthViewModelTest` uses MockK with `MainDispatcherRule` from `:core:testing` and drives the cooldown with `advanceTimeBy`.
