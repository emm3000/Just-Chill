package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.PlexMonoFontFamily
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
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
 * Renders the prefix at 40% of [size] in textTertiary, baseline-aligned with the integer.
 * The decimal part is rendered at the same [size] but at 0.55 alpha for visual hierarchy.
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
    val isNegative = value < 0
    val intPart = remember(absValue) {
        val formatter = NumberFormat.getIntegerInstance(Locale("es", "PE"))
        formatter.format(absValue.toLong())
    }
    val decPart: String? = remember(absValue, withDecimals) {
        if (withDecimals) {
            val formatter = DecimalFormat("00")
            formatter.format(((absValue - absValue.toLong()) * 100).toLong())
        } else null
    }

    val prefixSize = (size.value * 0.40f).sp
    val tightLetterSpacing = (-0.04 * size.value).sp

    // Prefix style — no tnum (S/ is not a digit; tnum widens non-digits in Plex Mono)
    val prefixStyle = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = prefixSize,
    )

    // Number style — full size with tnum + tight letter spacing
    val numberStyle = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontWeight = FontWeight.W500,
        fontFeatureSettings = "tnum",
        fontSize = size,
        letterSpacing = tightLetterSpacing,
    )

    val numberText: AnnotatedString = buildAnnotatedString {
        if (isNegative) append("−")
        append(intPart)
        if (decPart != null) {
            withStyle(SpanStyle(color = mainColor.copy(alpha = 0.55f))) {
                append(".$decPart")
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = prefix,
            style = prefixStyle,
            color = colors.textTertiary,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = numberText,
            style = numberStyle,
            color = mainColor,
            modifier = Modifier.alignByBaseline(),
        )
    }
}
