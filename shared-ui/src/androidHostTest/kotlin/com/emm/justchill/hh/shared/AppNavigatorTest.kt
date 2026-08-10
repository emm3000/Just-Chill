package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Guards the two navigation defects [AppNavigator] exists to close, plus the latent crash it fixed on
 * the way past.
 *
 * Nothing here needs a composition: the constructor is `internal` and takes `isReady` as a plain
 * lambda precisely so the guard logic can be driven from a JVM host test. [ready] stands in for nav3's
 * per-scene `LifecycleOwner` — `true` is a settled scene, `false` a scene mid-transition.
 *
 * [rootTab] is deliberately NOT the platform `startTab` (which is `SeeTransactionRoute` on Android):
 * the navigator must use the tab it was handed, not the global.
 *
 * What this does NOT cover: that the host actually builds its navigators inside `entry<...> { }`
 * bodies, which is what makes the transition guard live. That is a composition-level property, and the
 * comment on [AppNavigator] is what keeps it true.
 */
class AppNavigatorTest {

    private val rootTab: BottomBarRoute = HomeRoute
    private val backStack: NavBackStack<NavKey> = NavBackStack(rootTab)
    private var ready: Boolean = true
    private val navigator = AppNavigator(
        backStack = backStack,
        startTab = rootTab,
        isReady = { ready },
    )

    @Test
    fun `push puts the route on top`() {
        navigator.push(AddTransactionRoute)

        assertEquals(listOf<NavKey>(rootTab, AddTransactionRoute), backStack.toList())
    }

    @Test
    fun `push ignores a second tap on the route already on top`() {
        navigator.push(AddTransactionRoute)
        navigator.push(AddTransactionRoute)

        assertEquals(
            listOf<NavKey>(rootTab, AddTransactionRoute),
            backStack.toList(),
            "a double-tapped add button pushed the same key twice: nav3 renders one entry, back needs two presses",
        )
    }

    @Test
    fun `push adds a route that differs from the top only by value`() {
        navigator.push(EditTransactionRoute("tx-1"))
        navigator.push(EditTransactionRoute("tx-2"))

        assertEquals(
            listOf<NavKey>(rootTab, EditTransactionRoute("tx-1"), EditTransactionRoute("tx-2")),
            backStack.toList(),
            "the duplicate guard is by value: two different transactions are two legitimate entries",
        )
    }

    @Test
    fun `push refuses a route already deeper in the stack`() {
        navigator.push(AddTransactionRoute)
        navigator.push(CategoryRoute())
        val frozen: List<NavKey> = backStack.toList()

        navigator.push(AddTransactionRoute)

        assertEquals(
            frozen,
            backStack.toList(),
            "two entries with the same contentKey share a ViewModelStore and a SaveableStateHolder slot, " +
                "so the second screen opens holding the first one's state — distance down the stack changes nothing",
        )
    }

    /**
     * The site this subsumes. `AppNavHost` used to hand-roll `if (backStack.none { it is AuthRoute })`
     * around the sync handler's sign-in push, because that handler fires from wherever the user
     * happens to be. Keeping the rule in one place beats two places that agree.
     */
    @Test
    fun `push refuses a sign-in screen when one is already in the stack`() {
        navigator.push(AuthRoute)
        navigator.push(AddTransactionRoute)
        val frozen: List<NavKey> = backStack.toList()

        navigator.push(AuthRoute)

        assertEquals(frozen, backStack.toList(), "a second sign-in screen was stacked on the first")
    }

    @Test
    fun `no operation touches the stack while the scene is mid transition`() {
        navigator.push(AddTransactionRoute)
        navigator.push(CategoryRoute(propagateToTransaction = true))
        val frozen: List<NavKey> = backStack.toList()

        ready = false

        navigator.push(ReportRoute)
        assertEquals(frozen, backStack.toList(), "push ran mid-transition")
        navigator.pop()
        assertEquals(frozen, backStack.toList(), "pop ran mid-transition")
        navigator.switchTab(AccountsRoute)
        assertEquals(frozen, backStack.toList(), "switchTab ran mid-transition")
        navigator.replaceAll(ReportRoute)
        assertEquals(frozen, backStack.toList(), "replaceAll ran mid-transition")
        navigator.popToTransaction()
        assertEquals(frozen, backStack.toList(), "popToTransaction ran mid-transition")
    }

    @Test
    fun `pop removes the top entry`() {
        navigator.push(AddTransactionRoute)

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
        navigator.push(AddTransactionRoute)

        navigator.switchTab(AccountsRoute)

        assertEquals(listOf<NavKey>(rootTab, AccountsRoute), backStack.toList())
    }

    @Test
    fun `switchTab to the start tab leaves a single entry`() {
        navigator.push(AddTransactionRoute)

        navigator.switchTab(rootTab)

        assertEquals(listOf<NavKey>(rootTab), backStack.toList())
    }

    @Test
    fun `replaceAll drops everything below the new root`() {
        navigator.push(AddTransactionRoute)
        navigator.push(CategoryRoute())

        navigator.replaceAll(ReportRoute)

        assertEquals(listOf<NavKey>(ReportRoute), backStack.toList())
    }

    @Test
    fun `popToTransaction truncates back down to the transaction screen`() {
        navigator.push(AddTransactionRoute)
        navigator.push(CategoryRoute(propagateToTransaction = true))

        navigator.popToTransaction()

        assertEquals(listOf<NavKey>(rootTab, AddTransactionRoute), backStack.toList())
    }

    /**
     * The latent crash the predecessor carried. `popToTransactionScreen()` looped `removeLastOrNull()`
     * guarded only by `isNotEmpty()`, so a stack holding no transaction screen was drained to zero and
     * `NavDisplay`'s `require(backStack.isNotEmpty())` took the app down on the next composition.
     */
    @Test
    fun `popToTransaction leaves the stack untouched when no transaction screen is on it`() {
        navigator.push(CategoriesListRoute)
        navigator.push(CategoryRoute())
        val frozen: List<NavKey> = backStack.toList()

        navigator.popToTransaction()

        assertEquals(frozen, backStack.toList(), "with no transaction screen to reach, the stack was drained empty")
    }
}
