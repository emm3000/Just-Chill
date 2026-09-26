package com.emm.justchill.core.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

private data object HomeRoute : AppRoute

private data object ReportRoute : AppRoute

private data class CaptureFormRoute(val preselection: String? = null) : CaptureRoute

private data class CaptureDetailRoute(val id: String) : CaptureRoute

private data class PickerRoute(val propagates: Boolean = false) : AppRoute

private data object SignInRoute : AppRoute

private data object ListRoute : AppRoute

private data object DeepRoute : AppRoute

private data class DeepDetailRoute(val key: String) : AppRoute

private data class TabRoute(val name: String) : BottomBarRoute

class AppNavigatorTest {

    private val root: AppRoute = HomeRoute
    private val backStack: NavBackStack<NavKey> = NavBackStack(root)
    private var ready: Boolean = true
    private val navigator = AppNavigator(
        backStack = backStack,
        isReady = { ready },
    )
    private val listTab: TabRoute = TabRoute("list")
    private val reportTab: TabRoute = TabRoute("report")
    private val tabStack: NavBackStack<NavKey> = NavBackStack(listTab, reportTab)
    private val tabNavigator: AppNavigator = AppNavigator(backStack = tabStack, isReady = { ready })

    @Test
    fun `push puts the route on top`() {
        navigator.push(CaptureFormRoute())

        assertEquals(listOf<NavKey>(root, CaptureFormRoute()), backStack.toList())
    }

    @Test
    fun `push ignores a second tap on the route already on top`() {
        navigator.push(CaptureFormRoute())
        navigator.push(CaptureFormRoute())

        assertEquals(
            listOf<NavKey>(root, CaptureFormRoute()),
            backStack.toList(),
            "a double-tapped add button pushed the same key twice: nav3 renders one entry, back needs two presses",
        )
    }

    @Test
    fun `push adds a route that differs from the top only by value`() {
        navigator.push(CaptureDetailRoute("tx-1"))
        navigator.push(CaptureDetailRoute("tx-2"))

        assertEquals(
            listOf<NavKey>(root, CaptureDetailRoute("tx-1"), CaptureDetailRoute("tx-2")),
            backStack.toList(),
            "the duplicate guard is by value: two different transactions are two legitimate entries",
        )
    }

    @Test
    fun `push refuses a route already deeper in the stack`() {
        navigator.push(CaptureFormRoute())
        navigator.push(PickerRoute())
        val frozen: List<NavKey> = backStack.toList()

        navigator.push(CaptureFormRoute())

        assertEquals(
            frozen,
            backStack.toList(),
            "two entries with the same contentKey share a ViewModelStore and a SaveableStateHolder slot, " +
                "so the second screen opens holding the first one's state — distance down the stack changes nothing",
        )
    }

    @Test
    fun `push refuses a sign-in screen when one is already in the stack`() {
        navigator.push(SignInRoute)
        navigator.push(CaptureFormRoute())
        val frozen: List<NavKey> = backStack.toList()

        navigator.push(SignInRoute)

        assertEquals(frozen, backStack.toList(), "a second sign-in screen was stacked on the first")
    }

    @Test
    fun `no operation touches the stack while the scene is mid transition`() {
        navigator.push(CaptureFormRoute())
        navigator.push(PickerRoute(propagates = true))
        val frozen: List<NavKey> = backStack.toList()

        ready = false

        navigator.push(ReportRoute)
        assertEquals(frozen, backStack.toList(), "push ran mid-transition")
        navigator.pop()
        assertEquals(frozen, backStack.toList(), "pop ran mid-transition")
        navigator.replaceAll(ReportRoute)
        assertEquals(frozen, backStack.toList(), "replaceAll ran mid-transition")
        navigator.popToCapture()
        assertEquals(frozen, backStack.toList(), "popToCapture ran mid-transition")
        navigator.pushToTop(DeepRoute)
        assertEquals(frozen, backStack.toList(), "pushToTop ran mid-transition")
        navigator.selectTab(TabRoute("accounts"))
        assertEquals(frozen, backStack.toList(), "selectTab ran mid-transition")
    }

    @Test
    fun `pop removes the top entry`() {
        navigator.push(CaptureFormRoute())

        navigator.pop()

        assertEquals(listOf<NavKey>(root), backStack.toList())
    }

    @Test
    fun `pop refuses to empty the stack`() {
        navigator.pop()

        assertEquals(
            listOf<NavKey>(root),
            backStack.toList(),
            "NavDisplay opens with require(backStack.isNotEmpty()); an empty stack is a crash, not an exit",
        )
    }

    @Test
    fun `replaceAll drops everything below the new root`() {
        navigator.push(CaptureFormRoute())
        navigator.push(PickerRoute())

        navigator.replaceAll(ReportRoute)

        assertEquals(listOf<NavKey>(ReportRoute), backStack.toList())
    }

    @Test
    fun `popToCapture truncates back down to the capture screen`() {
        navigator.push(CaptureFormRoute())
        navigator.push(PickerRoute(propagates = true))

        navigator.popToCapture()

        assertEquals(listOf<NavKey>(root, CaptureFormRoute()), backStack.toList())
    }

    @Test
    fun `popToCapture returns to the nearest marked form, not to one buried under a list`() {
        navigator.push(CaptureDetailRoute("buried"))
        navigator.push(ListRoute)
        navigator.push(CaptureFormRoute())
        navigator.push(PickerRoute(propagates = true))

        navigator.popToCapture()

        assertEquals(
            listOf<NavKey>(root, CaptureDetailRoute("buried"), ListRoute, CaptureFormRoute()),
            backStack.toList(),
            "an unmarked form would have let the picker's return walk down to the buried capture",
        )
    }

    @Test
    fun `popToCapture leaves the stack untouched when no capture screen is on it`() {
        navigator.push(ListRoute)
        navigator.push(PickerRoute())
        val frozen: List<NavKey> = backStack.toList()

        navigator.popToCapture()

        assertEquals(frozen, backStack.toList(), "with no capture screen to reach, the stack was drained empty")
    }

    @Test
    fun `pushToTop reveals a buried route equal to the target, dropping only what sits above it`() {
        navigator.push(DeepRoute)
        navigator.push(DeepDetailRoute("carlos"))

        navigator.pushToTop(DeepRoute)

        assertEquals(
            listOf<NavKey>(root, DeepRoute),
            backStack.toList(),
            "push's contains guard leaves a buried route buried; a shortcut must still land on it",
        )
    }

    @Test
    fun `pushToTop is a no-op when the target is already on top`() {
        navigator.push(DeepRoute)
        val frozen: List<NavKey> = backStack.toList()

        navigator.pushToTop(DeepRoute)

        assertEquals(frozen, backStack.toList())
    }

    @Test
    fun `pushToTop pushes the route when it is nowhere in the stack`() {
        navigator.pushToTop(DeepRoute)

        assertEquals(listOf<NavKey>(root, DeepRoute), backStack.toList())
    }

    @Test
    fun `push can never reach the route the stack is rooted at`() {
        val homeStack: NavBackStack<NavKey> = NavBackStack(CaptureFormRoute())
        val homeNavigator = AppNavigator(backStack = homeStack, isReady = { ready })
        homeNavigator.push(ListRoute)

        homeNavigator.push(CaptureFormRoute())

        assertEquals(
            listOf<NavKey>(CaptureFormRoute(), ListRoute),
            homeStack.toList(),
            "the contains guard finds the home pad at index 0, so every push at it is a silent no-op",
        )
    }

    @Test
    fun `pushToTop reaches the route the stack is rooted at`() {
        val homeStack: NavBackStack<NavKey> = NavBackStack(CaptureFormRoute())
        val homeNavigator = AppNavigator(backStack = homeStack, isReady = { ready })
        homeNavigator.push(ListRoute)

        homeNavigator.pushToTop(CaptureFormRoute())

        assertEquals(
            listOf<NavKey>(CaptureFormRoute()),
            homeStack.toList(),
            "a screen sending the user home has to land on the root the app already holds",
        )
    }

    @Test
    fun `pushToTop onto a root of the same type replaces it and leaves one entry`() {
        val homeStack: NavBackStack<NavKey> = NavBackStack(CaptureFormRoute())
        val homeNavigator = AppNavigator(backStack = homeStack, isReady = { ready })
        homeNavigator.push(ListRoute)

        homeNavigator.pushToTop(CaptureFormRoute(preselection = "account-1"))

        assertEquals(
            listOf<NavKey>(CaptureFormRoute(preselection = "account-1")),
            homeStack.toList(),
            "a launcher combo has to land on the home pad carrying its preselect, with nothing left to go back to",
        )
    }

    @Test
    fun `pushToTop replaces a buried route of the same type but a different value`() {
        navigator.push(CaptureFormRoute(preselection = "account-1"))
        navigator.push(PickerRoute())

        navigator.pushToTop(CaptureFormRoute(preselection = "account-2"))

        assertEquals(
            listOf<NavKey>(root, CaptureFormRoute(preselection = "account-2")),
            backStack.toList(),
            "a different combo replaces the stale one instead of stacking a second CaptureFormRoute",
        )
    }

    @Test
    fun `selecting another tab swaps the tab above the root`() {
        tabNavigator.selectTab(TabRoute("accounts"))

        assertEquals(listOf<NavKey>(listTab, TabRoute("accounts")), tabStack.toList())
        assertSame(listTab, tabStack.first(), "the root entry was cleared and re-added, so its state died")
    }

    @Test
    fun `selecting the root tab drops back to the root alone`() {
        tabNavigator.selectTab(TabRoute("list"))

        assertEquals(listOf<NavKey>(listTab), tabStack.toList())
        assertSame(listTab, tabStack.first(), "the root entry was cleared and re-added, so its state died")
    }

    @Test
    fun `selecting the active tab changes nothing`() {
        tabNavigator.selectTab(TabRoute("report"))

        assertEquals(listOf<NavKey>(listTab, reportTab), tabStack.toList())
        assertSame(reportTab, tabStack.last(), "the active tab was popped and pushed again, so its state died")
    }
}
