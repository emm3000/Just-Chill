package com.emm.justchill.core.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Test
import kotlin.test.assertEquals

private data object HomeTab : BottomBarRoute

private data object AccountsTab : BottomBarRoute

private data object ReportTab : BottomBarRoute

private data class CaptureFormRoute(val preselection: String? = null) : CaptureRoute

private data class CaptureDetailRoute(val id: String) : CaptureRoute

private data class PickerRoute(val propagates: Boolean = false) : AppRoute

private data object SignInRoute : AppRoute

private data object ListRoute : AppRoute

private data object DeepRoute : AppRoute

private data class DeepDetailRoute(val key: String) : AppRoute

class AppNavigatorTest {

    private val rootTab: BottomBarRoute = HomeTab
    private val backStack: NavBackStack<NavKey> = NavBackStack(rootTab)
    private var ready: Boolean = true
    private val navigator = AppNavigator(
        backStack = backStack,
        startTab = rootTab,
        isReady = { ready },
    )

    @Test
    fun `push puts the route on top`() {
        navigator.push(CaptureFormRoute())

        assertEquals(listOf<NavKey>(rootTab, CaptureFormRoute()), backStack.toList())
    }

    @Test
    fun `push ignores a second tap on the route already on top`() {
        navigator.push(CaptureFormRoute())
        navigator.push(CaptureFormRoute())

        assertEquals(
            listOf<NavKey>(rootTab, CaptureFormRoute()),
            backStack.toList(),
            "a double-tapped add button pushed the same key twice: nav3 renders one entry, back needs two presses",
        )
    }

    @Test
    fun `push adds a route that differs from the top only by value`() {
        navigator.push(CaptureDetailRoute("tx-1"))
        navigator.push(CaptureDetailRoute("tx-2"))

        assertEquals(
            listOf<NavKey>(rootTab, CaptureDetailRoute("tx-1"), CaptureDetailRoute("tx-2")),
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

        navigator.push(ReportTab)
        assertEquals(frozen, backStack.toList(), "push ran mid-transition")
        navigator.pop()
        assertEquals(frozen, backStack.toList(), "pop ran mid-transition")
        navigator.switchTab(AccountsTab)
        assertEquals(frozen, backStack.toList(), "switchTab ran mid-transition")
        navigator.replaceAll(ReportTab)
        assertEquals(frozen, backStack.toList(), "replaceAll ran mid-transition")
        navigator.popToCapture()
        assertEquals(frozen, backStack.toList(), "popToCapture ran mid-transition")
        navigator.pushToTop(DeepRoute)
        assertEquals(frozen, backStack.toList(), "pushToTop ran mid-transition")
    }

    @Test
    fun `pop removes the top entry`() {
        navigator.push(CaptureFormRoute())

        navigator.pop()

        assertEquals(listOf<NavKey>(rootTab), backStack.toList())
    }

    @Test
    fun `pop refuses to empty the stack`() {
        navigator.pop()

        assertEquals(
            listOf<NavKey>(rootTab),
            backStack.toList(),
            "NavDisplay opens with require(backStack.isNotEmpty()); an empty stack is a crash, not an exit",
        )
    }

    @Test
    fun `switchTab roots the stack at the start tab and puts the target on top`() {
        navigator.push(CaptureFormRoute())

        navigator.switchTab(AccountsTab)

        assertEquals(listOf<NavKey>(rootTab, AccountsTab), backStack.toList())
    }

    @Test
    fun `switchTab to the start tab leaves a single entry`() {
        navigator.push(CaptureFormRoute())

        navigator.switchTab(rootTab)

        assertEquals(listOf<NavKey>(rootTab), backStack.toList())
    }

    @Test
    fun `replaceAll drops everything below the new root`() {
        navigator.push(CaptureFormRoute())
        navigator.push(PickerRoute())

        navigator.replaceAll(ReportTab)

        assertEquals(listOf<NavKey>(ReportTab), backStack.toList())
    }

    @Test
    fun `popToCapture truncates back down to the capture screen`() {
        navigator.push(CaptureFormRoute())
        navigator.push(PickerRoute(propagates = true))

        navigator.popToCapture()

        assertEquals(listOf<NavKey>(rootTab, CaptureFormRoute()), backStack.toList())
    }

    @Test
    fun `popToCapture returns to the nearest marked form, not to one buried under a list`() {
        navigator.push(CaptureDetailRoute("buried"))
        navigator.push(ListRoute)
        navigator.push(CaptureFormRoute())
        navigator.push(PickerRoute(propagates = true))

        navigator.popToCapture()

        assertEquals(
            listOf<NavKey>(rootTab, CaptureDetailRoute("buried"), ListRoute, CaptureFormRoute()),
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
            listOf<NavKey>(rootTab, DeepRoute),
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

        assertEquals(listOf<NavKey>(rootTab, DeepRoute), backStack.toList())
    }

    @Test
    fun `pushToTop replaces a buried route of the same type but a different value`() {
        navigator.push(CaptureFormRoute(preselection = "account-1"))
        navigator.push(PickerRoute())

        navigator.pushToTop(CaptureFormRoute(preselection = "account-2"))

        assertEquals(
            listOf<NavKey>(rootTab, CaptureFormRoute(preselection = "account-2")),
            backStack.toList(),
            "a different combo replaces the stale one instead of stacking a second CaptureFormRoute",
        )
    }
}
