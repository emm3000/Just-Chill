package com.emm.justchill.hh.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

@Stable
class AppNavigator internal constructor(
    private val backStack: NavBackStack<NavKey>,
    private val startTab: BottomBarRoute,
    private val isReady: () -> Boolean,
) {

    /**
     * Refuses [route] anywhere in the stack, not only on top: equal keys share a `contentKey`, and
     * both the saveable-state and the view-model decorators key on it — the second entry would open
     * holding the first one's content, saveable state and `ViewModelStore`.
     */
    fun push(route: AppRoute) {
        if (!isReady()) return
        if (backStack.contains(route)) return
        backStack.add(route)
    }

    /**
     * Never empties the stack: `NavDisplay` opens with `require(backStack.isNotEmpty())`, so reaching
     * zero crashes instead of closing the app.
     */
    fun pop() {
        if (!isReady()) return
        if (backStack.size <= 1) return
        backStack.removeLastOrNull()
    }

    fun switchTab(tab: BottomBarRoute) {
        if (!isReady()) return
        backStack.clear()
        backStack.add(startTab)
        if (tab != startTab) backStack.add(tab)
    }

    fun replaceAll(route: AppRoute) {
        if (!isReady()) return
        backStack.clear()
        backStack.add(route)
    }

    fun popToTransaction() {
        if (!isReady()) return
        val target: Int = backStack.indexOfLast { it is AddTransactionRoute || it is EditTransactionRoute }
        if (target < 0) return
        while (backStack.lastIndex > target) backStack.removeLastOrNull()
    }
}

/**
 * Call inside every `entry<...> { }` body that navigates: only there is `LocalLifecycleOwner` nav3's
 * per-scene owner, so the mid-transition guard can fire. Hoisted above `NavDisplay` it reads the
 * Activity's owner, which stays RESUMED, and only [AppNavigator.push]'s duplicate check survives.
 */
@Composable
fun rememberAppNavigator(backStack: NavBackStack<NavKey>): AppNavigator {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    return remember(backStack, startTab, lifecycleOwner) {
        AppNavigator(
            backStack = backStack,
            startTab = startTab,
            isReady = { lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) },
        )
    }
}
