package com.emm.justchill.hh.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * The single entry point for every navigation action [AppNavHost] performs.
 *
 * It replaces the raw `backStack.add(...)` / `backStack.removeLastOrNull()` calls that used to be
 * scattered across the host, each one free to reintroduce a bug someone had already fixed elsewhere.
 * Two of those bugs are closed structurally here:
 *
 *  - **Duplicate keys.** `NavEntry.contentKey` defaults to `key.toString()`, and nav3 documents that
 *    id as uniquely identifying both an entry's content and its decorator state — entries sharing one
 *    are handled as sharing content, one `SaveableStateHolder` slot and one `ViewModelStore`. A
 *    double-tapped "add" button pushed the same route twice: the first back press changed nothing
 *    visible and the user had to press it again. [push] keeps the whole stack free of duplicates.
 *  - **Cross-target races.** Two different list rows tapped in the same frame pushed
 *    `EditTransactionRoute("tx-1")` and `EditTransactionRoute("tx-2")`, and back then landed on a
 *    screen the user never asked for. Equal-by-type-but-not-by-value routes defeat the duplicate
 *    guard, so [isReady] handles this one: nothing dispatched from an unsettled scene is applied.
 *
 * ### Where you construct it decides how much of that is live
 *
 * [isReady] reads the ambient `LocalLifecycleOwner`, and only nav3 caps that owner per scene:
 * `NavDisplay` provides a scene-scoped `LifecycleOwner` held at [Lifecycle.State.STARTED] for as long
 * as the `AnimatedContent` transition runs, promoted to [Lifecycle.State.RESUMED] once it settles.
 *
 *  - Built INSIDE an `entry<...> { }` body it picks up that per-scene owner, and **both** guards are
 *    live.
 *  - Built at host level — outside `NavDisplay`, as the bottom bar and [SyncEventsHandler] call sites
 *    are — it picks up the Activity's owner, which stays `RESUMED` through all in-app navigation.
 *    Only the **duplicate-key** guard is live there; the transition guard can never fire and must not
 *    be relied on. That is not an oversight to fix by threading a lifecycle owner down: it is what
 *    "composed outside `NavDisplay`" means, and it is why the duplicate guard had to be a check that
 *    works without a lifecycle at all.
 *
 * @param backStack the stack every operation mutates.
 * @param startTab the bottom-bar tab the stack is rooted at; see [switchTab].
 * @param isReady whether the caller's scene has settled. Injected rather than read from the
 *   composition so the class is testable without one — [rememberAppNavigator] wires the real thing.
 */
@Stable
class AppNavigator internal constructor(
    private val backStack: NavBackStack<NavKey>,
    private val startTab: BottomBarRoute,
    private val isReady: () -> Boolean,
) {

    /**
     * Pushes [route], unless the scene is mid-transition or [route] is already anywhere in the stack.
     *
     * The invariant defended is that no two entries share a `contentKey`. nav3 documents that id as
     * unique per entry and treats entries that share one as sharing their content and their decorator
     * state, so a duplicate buried three screens down is the same defect as a duplicate on top: the
     * second screen would open holding the first one's form state. Refusing outright costs this app
     * nothing — its push graph is a DAG, so no reachable path wants the same key twice.
     *
     * The check is by VALUE, not by type: `EditTransactionRoute("tx-1")` and `("tx-2")` are two
     * distinct keys and both belong on the stack. Racing pushes of distinct keys are [isReady]'s job.
     *
     * Typed on [AppRoute] rather than `NavKey` so a key outside the closed route set — one that
     * `RouteSerializationTest` therefore never checked — cannot reach the back stack.
     */
    fun push(route: AppRoute) {
        if (!isReady()) return
        if (backStack.contains(route)) return
        backStack.add(route)
    }

    /**
     * Pops the top entry, refusing to empty the stack. `NavDisplay` opens with
     * `require(backStack.isNotEmpty())`, so a stack that reaches zero is a crash rather than a closed
     * app. System back does not come through here: `NavDisplay` owns it, and disables its own handler
     * once there is no previous entry.
     */
    fun pop() {
        if (!isReady()) return
        if (backStack.size <= 1) return
        backStack.removeLastOrNull()
    }

    /**
     * Switches to a bottom-bar tab using the "exit through [startTab]" pattern: the stack is rooted at
     * [startTab] with [tab] on top whenever the two differ, so back from any tab reaches the start tab
     * and then leaves the app. Idempotent by construction, hence no duplicate check.
     */
    fun switchTab(tab: BottomBarRoute) {
        if (!isReady()) return
        backStack.clear()
        backStack.add(startTab)
        if (tab != startTab) backStack.add(tab)
    }

    /**
     * Replaces the whole stack with [route]. Used by the first-launch Manifesto gate to land on
     * [startTab] with nothing behind it.
     */
    fun replaceAll(route: AppRoute) {
        if (!isReady()) return
        backStack.clear()
        backStack.add(route)
    }

    /**
     * Pops back to the nearest transaction screen, so it receives the just-created category through
     * its `LaunchedEffect` and the user lands where they left off. [EditTransactionRoute] counts as a
     * target even though nothing pushes a category from it today — the day something does, this keeps
     * working instead of draining the stack.
     *
     * Does **nothing** when no transaction screen is on the stack. Its predecessor looped
     * `removeLastOrNull()` guarded only by `isNotEmpty()`, so the same situation emptied the stack and
     * crashed `NavDisplay` on the next composition.
     */
    fun popToTransaction() {
        if (!isReady()) return
        val target: Int = backStack.indexOfLast { it is AddTransactionRoute || it is EditTransactionRoute }
        if (target < 0) return
        while (backStack.lastIndex > target) backStack.removeLastOrNull()
    }
}

/**
 * Remembers an [AppNavigator] over [backStack], bound to the lifecycle owner ambient at this call
 * site.
 *
 * Call it inside every `entry<...> { }` body that navigates. That is where `LocalLifecycleOwner` is
 * nav3's per-scene owner and the transition guard actually works; hoisting the call out of the entries
 * to save keystrokes silently downgrades every one of them to the duplicate-key guard alone. See
 * [AppNavigator] for what host-level construction does and does not buy.
 *
 * Keyed on the lifecycle owner deliberately: whether nav3 hands back a fresh owner per state or
 * mutates one stable instance, the remembered navigator reads the right lifecycle either way.
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
