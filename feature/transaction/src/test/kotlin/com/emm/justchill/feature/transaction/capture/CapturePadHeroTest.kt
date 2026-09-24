package com.emm.justchill.feature.transaction.capture

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class CapturePadHeroTest(private val width: Int, private val height: Int) {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `the hero has room for a full-size Plex Mono line at the default font scale`() {
        var heroFontSize: Dp = 0.dp
        composeRule.showPad(width, height, fontScale = 1f) { amountHero, density ->
            heroFontSize = with(density) { amountHero.fontSize.toDp() }
        }

        val hero: DpRect = composeRule.onNodeWithContentDescription("Gasto de S/ 0.00").getBoundsInRoot()
        val minHeight: Dp = heroFontSize * PLEX_MONO_LINE_BOX_EM

        assertTrue(hero.bottom - hero.top >= minHeight, "hero ${hero.bottom - hero.top} under $minHeight")
    }

    companion object {
        private const val PLEX_MONO_LINE_BOX_EM: Float = 1.3f

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}x{1}")
        fun cells(): List<Array<Any>> = PAD_WINDOWS.map { (width, height) -> arrayOf<Any>(width, height) }
    }
}
