package com.emm.justchill.feature.report.components

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.emm.justchill.core.ui.theme.EmmTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ComparisonPillTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `describes a rise with the verb Subió`() {
        assertEquals("Subió S/ 660, 12%", comparisonPillDescription("S/ 660", 12, directionUp = true))
    }

    @Test
    fun `describes a drop with the verb Bajó`() {
        assertEquals("Bajó S/ 660, 66%", comparisonPillDescription("S/ 660", 66, directionUp = false))
    }

    @Test
    fun `describes a capped percent through the label`() {
        assertEquals("Subió S/ 712, más de 999%", comparisonPillDescription("S/ 712", 44499, directionUp = true))
    }

    @Test
    fun `gives TalkBack the direction the arrow carries`() {
        renderPill(percent = 66, directionUp = false)

        composeRule.onNodeWithContentDescription("Bajó S/ 660, 66%").assertExists()
    }

    @Test
    fun `renders the capped percent on one line of pill text`() {
        renderPill(absoluteDeltaFormatted = "S/ 712", percent = 44499, directionUp = true)

        composeRule.onNodeWithText("S/ 712 · más de 999%", useUnmergedTree = true).assertExists()
    }

    private fun renderPill(absoluteDeltaFormatted: String = "S/ 660", percent: Int, directionUp: Boolean) {
        composeRule.setContent {
            EmmTheme {
                ComparisonPill(
                    absoluteDeltaFormatted = absoluteDeltaFormatted,
                    percent = percent,
                    directionUp = directionUp,
                    isPositive = true,
                )
            }
        }
    }
}
