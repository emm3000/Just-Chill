package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.PlexMonoFontFamily
import java.text.DecimalFormat
import kotlin.math.abs

enum class AmountTone {
    Neutral, // textPrimary
    Pos,     // success
    Neg,     // danger
    Mute,    // textTertiary
}

/**
 * Hero amount display in IBM Plex Mono with tabular figures.
 *
 * Renders: [prefix] at 40% of [size] in textTertiary, then the integer part + optional decimals.
 * Decimal part is rendered at 0.55 opacity for visual hierarchy.
 *
 * @param value        Numeric value (sign determines tone when [tone] is auto).
 * @param size         Base font size; defaults to 56sp. Use 48sp for detail screens.
 * @param tone         Color tone for the main number.
 * @param withDecimals If true, renders ".XX" decimals (0.55 opacity).
 * @param prefix       Currency prefix, e.g. "S/".
 */
@Composable
fun AmountHero(
    value: Double,
    size: TextUnit = 56.sp,
    tone: AmountTone = AmountTone.Neutral,
    withDecimals: Boolean = true,
    prefix: String = "S/",
) {
    val colors = LocalEmmColors.current

    val mainColor: Color = when (tone) {
        AmountTone.Neutral -> colors.textPrimary
        AmountTone.Pos -> colors.success
        AmountTone.Neg -> colors.danger
        AmountTone.Mute -> colors.textTertiary
    }

    val absValue = abs(value)
    val intPart = remember(absValue) { absValue.toLong().toString() }
    val decPart = remember(absValue, withDecimals) {
        if (withDecimals) {
            val formatter = DecimalFormat("00")
            formatter.format(((absValue - absValue.toLong()) * 100).toLong())
        } else null
    }

    val prefixSize = (size.value * 0.40f).sp
    val monoStyle = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontWeight = FontWeight.W500,
        fontFeatureSettings = "tnum",
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Prefix "S/"
        Text(
            text = prefix,
            style = monoStyle.copy(fontSize = prefixSize),
            color = colors.textTertiary,
        )

        // Integer part
        Text(
            text = intPart,
            style = monoStyle.copy(fontSize = size),
            color = mainColor,
        )

        // Decimal part
        if (decPart != null) {
            Text(
                text = ".$decPart",
                style = monoStyle.copy(fontSize = prefixSize),
                color = mainColor,
                modifier = Modifier.alpha(0.55f),
            )
        }
    }
}
