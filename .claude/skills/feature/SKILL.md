---
name: feature
description: Scaffold a new :feature:<name> module with its MVI shape, route registry and app wiring
argument-hint: <FeatureName>
allowed-tools: Read Write Edit Bash(fd:*) Bash(rg:*) Bash(./gradlew:*)
disable-model-invocation: true
---

Scaffold a new feature called **$ARGUMENTS** as its own Gradle module, `:feature:<feature>` (ADR 015). A feature owns its ViewModels, screens, routes and Koin module; `:androidApp` owns the wiring that reaches it.

## Before creating anything

1. Confirm the name is PascalCase and that `feature/<feature>/` does not exist yet (`fd`).
2. Read `:feature:auth` end to end — it is the reference shape (sealed state, intent grouping, effect consumption, the re-entrancy guard in state).
3. Confirm with me which existing feature you used as the template.

## The module

| Path | Content |
|---|---|
| `feature/<feature>/build.gradle.kts` | `build.gradle.kts.template` — `justchill.android.feature` and nothing else unless a library is actually used |
| `feature/<feature>/.gitignore` | `gitignore.template` — `/build`; the root `.gitignore` covers only the top level |
| `feature/<feature>/CLAUDE.md` | a short doc: what the feature owns, its Koin and route wiring, its gotchas |

Add `include(":feature:<feature>")` to `settings.gradle.kts` and `implementation(projects.feature.<feature>)` to `androidApp/build.gradle.kts`.

A `compose_stability.conf` is only for a module whose screens read state declared elsewhere; copy `feature/report/`'s if that applies.

## Kotlin files

Copy each template from `${CLAUDE_SKILL_DIR}/templates/<file>` into `feature/<feature>/src/main/kotlin/com/emm/justchill/feature/<feature>/`, drop the `.template` suffix and substitute every placeholder:

| Placeholder | Replace with |
|---|---|
| `__Feature__` | the PascalCase name **$ARGUMENTS** |
| `__feature__` | the same name lowercased, the package segment and the Koin module prefix |

`UiState` (data class, initial values, no logic), `Intent` (sealed interface of what the user did), `Effect` (sealed interface of one-shot effects, `ShowError(DomainException)` included), `ViewModel` (`MviViewModel<S, I, E>`, `onIntent` the single entry point, `launchSafe` for anything that can fail), `Routes` (the `@Serializable` routes plus the `<feature>Routes` registry), `Module` (`<feature>Module` binding ViewModels only), `Screen` (collects state, consumes effects, hands `vm::onIntent` to a private stateless content composable with `@Preview`s) and `Entries` (`entry<Route>` obtaining an `AppNavigator`, passing host state as lambdas).

The templates are the minimal shape: one state field, one intent, two effects. Grow them to fit the feature without changing the idioms they encode.

Placeholder copy in the screen is a literal Spanish string, tuteo. A message a ViewModel emits is an enum resolved to text in the screen, never a string in state or effects.

## Wiring in `:androidApp`

1. `wiring/$ARGUMENTSWiring.kt` — `includes(<feature>Module)` plus this feature's use cases, and add it to `appModules()` (`androidApp/.../core/AppGraph.kt`). Skip the file only if the feature injects nothing.
2. Add `$ARGUMENTSViewModel::class` to `EXPECTED_VIEW_MODELS` in `androidApp/src/test/.../core/AppGraphKoinTest.kt`.
3. Add `<feature>Routes` to the registry list in `androidApp/src/test/.../shell/RouteSerializationTest.kt`, and one sample per route to its `samples` map with non-default field values.
4. Call `<feature>Entries(bindings)` inside `entryProvider { }` in `androidApp/.../shell/AppNavHost.kt`.
5. Give the route a door: a `nav.push($ARGUMENTSRoute)` from an existing entry, and a second one if the first is gated (`architecture.md`, one door is no door). Navigation out of the feature arrives as an `(AppNavigator) -> Unit` callback the host supplies, never as a route value.

## Hard rules (from `CLAUDE.md` and `.claude/rules/`)

- UI uses **only** `core/ui/atoms/` components; `Text` and `Icon` only with a `LocalEmmType` role and a `LocalEmmColors` token. Never a raw Material3 control.
- A feature depends on `:core:ui` and `:core:domain` only, plus `:core:testing` on the test edge; `checkModuleBoundaries` fails anything else.
- No Compose import in a ViewModel or a UiState; `checkComposeFreeViewModels` is on the gate.
- Explicit types on every property and local; no comments.
- At most 4 levels of nesting, no nested `also/apply/run/let`, ≤ 2 real returns per function — review-enforced, see `.claude/rules/kotlin-style.md`.

After creating the files, run `./gradlew qualityGate assembleDevDebug`.
