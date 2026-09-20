package com.emm.justchill.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class EmmColorsContrastTest {

    @Test
    fun `borderFocus clears the 3 to 1 boundary ratio against the background`() {
        val ratio: Double = contrastRatio(emmDarkColors.borderFocus, emmDarkColors.bg)

        assertTrue(ratio >= NON_TEXT_MINIMUM_RATIO, "borderFocus vs bg is $ratio, below $NON_TEXT_MINIMUM_RATIO")
    }

    @Test
    fun `every readable text token clears the 4 dot 5 to 1 ratio against the background`() {
        val readableTokens: Map<String, Color> = mapOf(
            "textPrimary" to emmDarkColors.textPrimary,
            "textSecondary" to emmDarkColors.textSecondary,
            "textTertiary" to emmDarkColors.textTertiary,
        )

        readableTokens.forEach { (name: String, token: Color) ->
            val ratio: Double = contrastRatio(token, emmDarkColors.bg)

            assertTrue(ratio >= TEXT_MINIMUM_RATIO, "$name vs bg is $ratio, below $TEXT_MINIMUM_RATIO")
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val lighter: Double = maxOf(relativeLuminance(foreground), relativeLuminance(background))
        val darker: Double = minOf(relativeLuminance(foreground), relativeLuminance(background))

        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val red: Double = linearize(color.red)
        val green: Double = linearize(color.green)
        val blue: Double = linearize(color.blue)

        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun linearize(channel: Float): Double {
        val value: Double = channel.toDouble()

        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private companion object {
        const val NON_TEXT_MINIMUM_RATIO: Double = 3.0
        const val TEXT_MINIMUM_RATIO: Double = 4.5
    }
}
