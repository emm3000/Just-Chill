package com.emm.justchill.hh.shared

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * The four host-level things every feature's nav entries bind to.
 *
 * [AppNavHost] builds exactly one of these and hands the same instance to each
 * `EntryProviderScope<NavKey>.xxxEntries(...)` function.
 *
 * ### Why a bundle instead of four parameters
 *
 * detekt's `LongParameterList` allows five function parameters and does not exempt defaulted ones
 * (`ignoreDefaultParameters: false`). Passed loose, these four would leave a single slot, and the
 * profile entries alone need three more (`appVersion` plus the pending-import result channel) — six
 * parameters before any other feature is considered. Bundling the shared four is what keeps every
 * entries function under that ceiling without a suppression.
 *
 * ### Why it is not a god object
 *
 * Only what *every* feature binds to lives here. Anything a single feature needs stays an explicit
 * parameter on that feature's entries function, where the signature still says which feature depends
 * on it — see `profileEntries`, `categoryEntries`, `transactionEntries` and `onboardingEntries`.
 *
 * @param backStack the stack each entry builds its own in-scene [AppNavigator] over. Call
 *   [rememberAppNavigator] inside the `entry<...> { }` body, never above it: that is where
 *   `LocalLifecycleOwner` is nav3's per-scene owner and the transition guard is live.
 * @param snackbarHostState the root `Scaffold`'s host, for screens that show their own snackbars.
 * @param showMessage shows a plain message on that same root host from a non-composable callback.
 * @param platform the per-platform capability flags and actions; see [PlatformHostActions].
 */
@Stable
class NavHostBindings(
    val backStack: NavBackStack<NavKey>,
    val snackbarHostState: SnackbarHostState,
    val showMessage: (String) -> Unit,
    val platform: PlatformHostActions,
)
