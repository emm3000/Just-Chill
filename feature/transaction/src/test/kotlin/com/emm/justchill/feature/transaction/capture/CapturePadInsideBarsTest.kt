package com.emm.justchill.feature.transaction.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpRect
import com.emm.justchill.core.ui.NumpadKeyHeight
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h1000dp")
class CapturePadInsideBarsTest(private val width: Int, private val height: Int) {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `a phone window inside its system bars keeps full keys and the combos eyebrow`() {
        composeRule.showPad(width, height, fontScale = 1f)

        val key: DpRect = composeRule.onNodeWithText("1").getBoundsInRoot()

        assertEquals(NumpadKeyHeight, key.bottom - key.top)
        composeRule.onNodeWithText("TUS COMBINACIONES FRECUENTES").assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}x{1}")
        fun cells(): List<Array<Any>> = REDMI_INSIDE_BARS_WINDOWS.map { (width, height) -> arrayOf<Any>(width, height) }
    }
}
