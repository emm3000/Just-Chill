package com.emm.justchill.feature.transaction.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmType
import org.junit.Assume.assumeTrue
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
        showPad()

        val frame: DpRect = composeRule.onNodeWithTag(FRAME_TAG).getBoundsInRoot()
        val cta: DpRect = composeRule.onNodeWithText("Anotar gasto").assertIsDisplayed().getBoundsInRoot()

        assertTrue(cta.bottom <= frame.bottom, "CTA bottom ${cta.bottom} past frame bottom ${frame.bottom}")
    }

    @Test
    fun `the hero keeps its full line height at the default font scale`() {
        assumeTrue(fontScale == 1f)
        var heroLineHeight: Dp = 0.dp
        showPad { lineHeight, density -> heroLineHeight = with(density) { lineHeight.toDp() } }

        val hero: DpRect = composeRule.onNodeWithContentDescription("Gasto de S/ 0.00").getBoundsInRoot()

        assertTrue(hero.bottom - hero.top >= heroLineHeight, "hero ${hero.bottom - hero.top} under $heroLineHeight")
    }

    @Test
    fun `every key keeps a full touch target`() {
        showPad()

        KEY_LABELS.forEach { label: String ->
            val key: DpRect = composeRule.onNode(hasText(label) or hasContentDescription(label)).getBoundsInRoot()
            assertTrue(key.right - key.left >= TOUCH_TARGET, "key $label is ${key.right - key.left} wide")
            assertTrue(key.bottom - key.top >= TOUCH_TARGET, "key $label is ${key.bottom - key.top} tall")
        }
    }

    private fun showPad(onHeroLineHeight: (TextUnit, Density) -> Unit = { _, _ -> }) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, height.dp)) then
                    DeviceConfigurationOverride.FontScale(fontScale),
            ) {
                EmmTheme {
                    onHeroLineHeight(LocalEmmType.current.amountHero.lineHeight, LocalDensity.current)
                    Box(modifier = Modifier.fillMaxSize().testTag(FRAME_TAG)) {
                        AddTransactionScreenContent(
                            state = populatedCaptureState(),
                            onIntent = {},
                            onOpenMenu = {},
                            onOpenTransactions = {},
                            onSave = {},
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val FRAME_TAG: String = "frame"
        private val TOUCH_TARGET: Dp = 48.dp
        private val KEY_LABELS: List<String> =
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "Borrar", "Cambiar a ingreso")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}x{1} at font scale {2}")
        fun cells(): List<Array<Any>> = listOf(
            360 to 640,
            360 to 568,
            320 to 640,
            800 to 360,
            360 to 350,
        ).flatMap { (width, height) ->
            listOf(1f, 2f).map { scale -> arrayOf<Any>(width, height, scale) }
        }
    }
}
