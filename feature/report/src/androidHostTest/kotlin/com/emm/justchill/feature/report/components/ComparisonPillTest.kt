package com.emm.justchill.feature.report.components

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.emm.justchill.core.presentation.format.CURRENCY_PREFIX
import com.emm.justchill.core.ui.theme.EmmTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ComparisonPillTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `gives TalkBack the description the state carries`() {
        renderPill(description = "Bajó ${CURRENCY_PREFIX}660, 66%", directionUp = false)

        composeRule.onNodeWithContentDescription("Bajó ${CURRENCY_PREFIX}660, 66%").assertExists()
    }

    @Test
    fun `renders the text the state carries`() {
        renderPill(text = "${CURRENCY_PREFIX}712 · más de 999%", directionUp = true)

        composeRule.onNodeWithText("${CURRENCY_PREFIX}712 · más de 999%", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `renders an unchanged month with no direction`() {
        renderPill(text = "${CURRENCY_PREFIX}0 · 0%", description = "Sin cambio, 0%", directionUp = null)

        composeRule.onNodeWithContentDescription("Sin cambio, 0%").assertExists()
    }

    private fun renderPill(
        text: String = "${CURRENCY_PREFIX}660 · 66%",
        description: String = "Bajó ${CURRENCY_PREFIX}660, 66%",
        directionUp: Boolean?,
    ) {
        composeRule.setContent {
            EmmTheme {
                ComparisonPill(
                    text = text,
                    description = description,
                    directionUp = directionUp,
                    isPositive = directionUp?.let { true },
                )
            }
        }
    }
}
