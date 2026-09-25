package com.emm.justchill.feature.onboarding

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.emm.justchill.core.ui.navigation.AppRoute
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.PlatformHostActions
import com.emm.justchill.core.ui.theme.EmmTheme
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
private data object HomeRoute : AppRoute

@Serializable
private data object ProfileRoute : NavKey

private object SilentPlatformHostActions : PlatformHostActions {
    override val showGoogleSignIn: Boolean = false
    override val supportsPrivacyPolicy: Boolean = false
    override val supportsBackup: Boolean = false
    override val onShareText: (String) -> Unit = {}
    override val onOpenEmailApp: () -> Unit = {}
    override val requestExport: (String, (Boolean) -> Unit) -> Unit = { _, _ -> }
    override val requestImport: () -> Unit = {}
    override val shareCsv: (String, String, () -> Unit) -> Unit = { _, _, _ -> }
}

@RunWith(RobolectricTestRunner::class)
class OnboardingEntriesTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private var firstLaunchSeenWrites: Int = 0

    @Test
    fun `starting from the first launch marks the manifesto seen and roots the stack at the home pad`() {
        val backStack: NavBackStack<NavKey> = NavBackStack(ManifestoRoute())
        renderManifestoEntry(backStack)

        composeRule.onNodeWithText("Empezar").performClick()

        assertEquals(1, firstLaunchSeenWrites)
        assertEquals(listOf<NavKey>(HomeRoute), backStack.toList())
    }

    @Test
    fun `returning from a revisit pops the manifesto and never marks it seen`() {
        val backStack: NavBackStack<NavKey> = NavBackStack(HomeRoute, ProfileRoute, ManifestoRoute(isRevisit = true))
        renderManifestoEntry(backStack)

        composeRule.onNodeWithText("Volver").performClick()

        assertEquals(0, firstLaunchSeenWrites)
        assertEquals(listOf<NavKey>(HomeRoute, ProfileRoute), backStack.toList())
    }

    private fun renderManifestoEntry(backStack: NavBackStack<NavKey>) {
        val bindings: NavHostBindings = NavHostBindings(
            backStack = backStack,
            snackbarHostState = SnackbarHostState(),
            showMessage = {},
            platform = SilentPlatformHostActions,
        )
        val resolveEntry: (NavKey) -> NavEntry<NavKey> = entryProvider {
            onboardingEntries(bindings, home = HomeRoute, onFirstLaunchSeen = { firstLaunchSeenWrites += 1 })
        }
        val manifestoEntry: NavEntry<NavKey> = resolveEntry(backStack.last())

        composeRule.setContent {
            EmmTheme {
                manifestoEntry.Content()
            }
        }
    }
}
