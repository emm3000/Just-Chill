---
name: feature
description: Scaffold a new MVI feature across :presentation and :ui-android following the repo rules
argument-hint: <FeatureName>
allowed-tools: Read Write Edit Bash(fd:*) Bash(rg:*) Bash(./gradlew:*)
disable-model-invocation: true
---

New features go into `:feature:<name>` per ADR 015; the scaffold below is legacy, for the five features (transaction, recurring, profile, auth, onboarding) still in `:presentation` / `:ui-android` until #128.

Scaffold a new feature called **$ARGUMENTS** across the two modules that own a feature:

- `presentation/src/main/kotlin/com/emm/justchill/hh/<feature>/` — `UiState`, `Intent`, `Effect`, `ViewModel`.
- `presentation/src/main/kotlin/com/emm/justchill/hh/di/` — its Koin module.
- `ui-android/src/main/kotlin/com/emm/justchill/hh/<feature>/` — `Screen` and `Entries`.
- `ui-android/src/main/kotlin/com/emm/justchill/hh/shared/HhRoutes.kt` — its `@Serializable` route.

## Before creating anything

1. Confirm the feature name is PascalCase and that neither `hh/<feature>/` directory exists yet (`fd`).
2. Read `hh/auth/` in both modules and `hh/di/AuthModule.kt` to copy the idioms (sealed state, intent grouping, effect consumption, `koinViewModel()` default parameter).
3. Confirm with me which existing feature you used as the template.

## Files to create

Copy each template from `${CLAUDE_SKILL_DIR}/templates/<file>` into its module, drop the `.template` suffix and substitute every placeholder:

| Placeholder | Replace with |
|---|---|
| `__Feature__` | the PascalCase name **$ARGUMENTS** |
| `__feature__` | the same name lowercased, used as the package segment and the Koin module prefix |

| Template | Lands in |
|---|---|
| `__Feature__UiState.kt.template` | `presentation/.../hh/<feature>/` — `data class` with initial values, no logic |
| `__Feature__Intent.kt.template` | same — `sealed interface` covering what the user did |
| `__Feature__Effect.kt.template` | same — `sealed interface` for one-shot effects, `ShowError(DomainException)` included |
| `__Feature__ViewModel.kt.template` | same — `MviViewModel<S, I, E>`, `onIntent` as the single entry point, `launchSafe` for anything that can fail |
| `__Feature__Module.kt.template` | `presentation/.../hh/di/` — `viewModel { }` with one `get()` per constructor dependency |
| `__Feature__Screen.kt.template` | `ui-android/.../hh/<feature>/` — collects state, consumes effects, hands `vm::onIntent` to a private stateless content composable with `@Preview`s |
| `__Feature__Entries.kt.template` | same — `entry<Route>` obtaining an `AppNavigator`, passing host state as lambdas |

The templates are the minimal shape: one state field, one intent, two effects. Grow them to fit the feature without changing the idioms they encode.

Placeholder copy in the screen is a literal Spanish string, tuteo. Shared copy goes through `:presentation`'s `hh/shared/UiStrings.kt`; a message a ViewModel emits is an enum resolved to text in the screen, never a string in state or effects.

## Wiring

1. Add `@Serializable data object $ARGUMENTSRoute : AppRoute` to `hh/shared/HhRoutes.kt` (a `data class` when it carries arguments; every field serializable). `RouteSerializationTest` picks it up.
2. Add `<feature>Module` to the list in `appModules()` (`androidApp/.../core/AppGraph.kt`).
3. Add `$ARGUMENTSViewModel::class` to `EXPECTED_VIEW_MODELS` in `androidApp/src/test/.../core/AppGraphKoinTest.kt`.
4. Call `<feature>Entries(bindings)` inside `entryProvider { }` in `androidApp/.../shell/AppNavHost.kt`.
5. Give the route a door: a `nav.push($ARGUMENTSRoute)` from an existing entry, and a second one if the first is gated (`architecture.md`, one door is no door).

## Hard rules (from `CLAUDE.md` and `.claude/rules/`)

- UI uses **only** `core/ui/atoms/` components; `Text` and `Icon` only with a `LocalEmmType` role and a `LocalEmmColors` token. Never a raw Material3 control.
- No Compose import in `:presentation`; no `:core:domain` repository import in `:ui-android`.
- Explicit types on every property and local; no comments.
- At most 4 levels of nesting, no nested `also/apply/run/let`, ≤ 2 real returns per function — review-enforced, see `.claude/rules/kotlin-style.md`.

After creating the files, run `./gradlew :androidApp:testDevDebugUnitTest` (the Koin graph test and the route test) and `./gradlew assembleDevDebug`.
