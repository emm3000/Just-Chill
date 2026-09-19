package com.emm.justchill.feature.onboarding

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.emm.justchill.core.ui.theme.EmmTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ManifestoScreenTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private var startClicks: Int = 0

    @Test
    fun `renders the first launch call to action with a click action`() {
        renderManifesto(isRevisit = false)

        composeRule.onNodeWithText("Empezar").assertHasClickAction()
    }

    @Test
    fun `clicking the first launch call to action invokes onStart exactly once`() {
        renderManifesto(isRevisit = false)

        composeRule.onNodeWithText("Empezar").performClick()

        assertEquals(1, startClicks)
    }

    @Test
    fun `renders the revisit call to action as Volver with a click action`() {
        renderManifesto(isRevisit = true)

        composeRule.onNodeWithText("Volver").assertHasClickAction()
    }

    @Test
    fun `the revisit call to action never keeps the first launch label`() {
        renderManifesto(isRevisit = true)

        composeRule.onNodeWithText("Empezar").assertDoesNotExist()
    }

    @Test
    fun `clicking the revisit call to action invokes onStart exactly once`() {
        renderManifesto(isRevisit = true)

        composeRule.onNodeWithText("Volver").performClick()

        assertEquals(1, startClicks)
    }

    @Test
    fun `renders the manifesto copy on the first launch branch`() {
        renderManifesto(isRevisit = false)

        composeRule.onNodeWithText("Solo tú, tu plata,\ny la verdad.").assertExists()
    }

    @Test
    fun `renders the manifesto copy on the revisit branch`() {
        renderManifesto(isRevisit = true)

        composeRule.onNodeWithText("Solo tú, tu plata,\ny la verdad.").assertExists()
    }

    private fun renderManifesto(isRevisit: Boolean) {
        composeRule.setContent {
            EmmTheme {
                ManifestoScreen(isRevisit = isRevisit, onStart = { startClicks += 1 })
            }
        }
    }
}
