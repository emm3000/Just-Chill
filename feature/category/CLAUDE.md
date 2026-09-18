# :feature:category — CLAUDE.md

The Categories screen family, ViewModels and Compose together: the list with its edit and delete dialogs, the create screen with the icon and colour pickers and the preview chip, and the two routes the host pushes. Extracted by ADR 015's wave 7 (#119); the package is `com.emm.justchill.feature.category`.

`justchill.android.feature` is the whole build file, plus `androidx.lifecycle.runtime.compose` and `androidx.material.icons.extended`, which the plugin does not bring and the screens need. Depends on `:core:domain` and `:core:ui` only. `compose_stability.conf` declares `com.emm.justchill.**` stable so the `:core:domain` values the screens take are not treated as unstable.

## What lives where

- `CategoryRoutes.kt` — `CategoriesListRoute` and `CategoryRoute`, both `@Serializable` over `:core:ui`'s `AppRoute`, and `categoryRoutes`, the registry `:androidApp`'s `RouteSerializationTest` concatenates. A route missing from it is never round-tripped, so it crashes on process-death restore and nowhere else.
- `CategoryEntries.kt` — `categoryEntries(bindings, onCategoryForTransaction)`. Each entry body calls `rememberAppNavigator(bindings.backStack, bindings.startTab)` itself: hoisting it would read the Activity's lifecycle owner instead of nav3's per-scene one and silently drop the mid-transition guard.
- `CategoryModule.kt` — `categoryModule`, the two ViewModels and nothing else. The use cases are bound in `:androidApp`'s `wiring/CategoryWiring.kt`, which `includes` it.

## Gotchas

- `AddCategoryViewModel` takes `initialType` and `initialName` as Koin parameters, so it is a `viewModel { parameters -> }` block, never `viewModelOf`. The entry passes them with `parametersOf(key.initialType, key.initialName)`.
- Saving with `propagateToTransaction` calls `AppNavigator.popToCapture()`, which pops to whatever implements `:core:ui`'s `CaptureRoute`. The feature never names a transaction route, and the created category reaches the capture screen through the host's `pendingCategory` channel, not through the back stack.
- Write `""`, never a `String.Empty` extension; that one is gone.
- `CategoriesScreen`'s edit and delete dialogs are built on `:core:ui`'s `EmmDialog` atom (#194), general enough for another feature's confirmation dialog to reuse.

## Testing

`./gradlew :feature:category:testDebugUnitTest`. `DeleteCategoryCopyTest` pins the Spanish delete copy, including the zero-movement branch that stops a harmless cleanup from reading as a history wipe. `MainDispatcherRule` comes from `:core:testing` when a ViewModel test here touches `viewModelScope`; the Koin graph and route serialization suites stay in `:androidApp`.
