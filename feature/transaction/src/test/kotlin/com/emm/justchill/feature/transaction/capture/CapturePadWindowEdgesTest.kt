package com.emm.justchill.feature.transaction.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h1000dp")
class CapturePadWindowEdgesTest(private val width: Int, private val height: Int, private val fontScale: Float) {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `the save button sits whole inside the window`() {
        composeRule.showPad(width, height, fontScale)

        val frame: DpRect = composeRule.onNodeWithTag(PAD_FRAME_TAG).getBoundsInRoot()
        val cta: DpRect = composeRule.onNodeWithText("Anotar gasto").assertIsDisplayed().getBoundsInRoot()

        assertTrue(cta.bottom <= frame.bottom, "CTA bottom ${cta.bottom} past frame bottom ${frame.bottom}")
    }

    @Test
    fun `every key keeps a full touch target`() {
        composeRule.showPad(width, height, fontScale)

        KEY_LABELS.forEach { label: String ->
            val key: DpRect = composeRule.onNode(hasText(label) or hasContentDescription(label)).getBoundsInRoot()
            assertTrue(key.right - key.left >= TOUCH_TARGET, "key $label is ${key.right - key.left} wide")
            assertTrue(key.bottom - key.top >= TOUCH_TARGET, "key $label is ${key.bottom - key.top} tall")
        }
    }

    @Test
    fun `both sign segments keep a full touch target`() {
        composeRule.showPad(width, height, fontScale)

        SIGN_SEGMENTS.forEach { label: String ->
            val segment: DpRect = composeRule.onNode(hasText(label) and hasClickAction()).getBoundsInRoot()
            val segmentWidth: Dp = segment.right - segment.left
            val segmentHeight: Dp = segment.bottom - segment.top
            assertTrue(segmentWidth >= TOUCH_TARGET, "segment $label is $segmentWidth wide")
            assertTrue(segmentHeight >= TOUCH_TARGET, "segment $label is $segmentHeight tall")
        }
    }

    companion object {
        private val TOUCH_TARGET: Dp = 48.dp
        private val KEY_LABELS: List<String> =
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "Borrar")
        private val SIGN_SEGMENTS: List<String> = listOf("Ingreso", "Gasto")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}x{1} at font scale {2}")
        fun cells(): List<Array<Any>> = PAD_WINDOWS.flatMap { (width, height) ->
            listOf(1f, 2f).map { scale -> arrayOf<Any>(width, height, scale) }
        }
    }
}
