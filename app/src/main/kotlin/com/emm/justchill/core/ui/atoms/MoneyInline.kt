package com.emm.justchill.core.ui.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.PlexMonoFontFamily
import java.text.DecimalFormat
import kotlin.math.abs

/**
 * Inline monetary amount — 15sp IBM Plex Mono w500, tabular figures.
 *
 * Negative values are prefixed with a minus sign (−).
 * Format: "S/ 1,234.56" or "− S/ 1,234.56"
 *
 * @param value   Numeric amount. Sign is retained in the rendered string.
 * @param color   Override text color; defaults to [EmmColors.textPrimary].
 * @param weight  Font weight; defaults to [FontWeight.Medium].
 */
@Composable
fun MoneyInline(
    value: Double,
    color: Color? = null,
    weight: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    val formatted = remember(value) {
        val formatter = DecimalFormat("#,##0.00")
        val prefix = if (value < 0) "− S/ " else "S/ "
        "$prefix${formatter.format(abs(value))}"
    }

    Text(
        text = formatted,
        style = TextStyle(
            color = color ?: colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = weight,
            fontFamily = PlexMonoFontFamily,
            fontFeatureSettings = "tnum",
        ),
        modifier = modifier,
    )
}
