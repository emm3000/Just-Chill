package com.emm.justchill.core.ui.atoms

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.PlexMonoFontFamily
import com.emm.justchill.hh.shared.NumberFormatEs
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Hero amount display in IBM Plex Mono with tabular figures.
 *
 * Renders the prefix at 40% of [size] in textTertiary, baseline-aligned with the integer.
 * The decimal part is rendered at the same [size] but at 0.55 alpha for visual hierarchy.
 *
 * When [showCaret] is true a thin blinking caret is rendered right after the last digit as an
 * input affordance — it signals "this is a live field, type here". It never moves: the amount is a
 * cents accumulator (digits enter at the right edge), so there are no caret positions to move to.
 */
@Composable
fun AmountHero(
    value: Double,
    modifier: Modifier = Modifier,
    size: TextUnit = 56.sp,
    tone: AmountTone = AmountTone.Neutral,
    withDecimals: Boolean = true,
    prefix: String = "S/",
    showCaret: Boolean = false,
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
        NumberFormatEs.integer(absValue.toLong())
    }
    val decPart: String? = remember(absValue, withDecimals) {
        if (withDecimals) {
            ((absValue - absValue.toLong()) * 100).roundToLong().toString().padStart(2, '0')
        } else {
            null
        }
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
        if (showCaret) {
            appendInlineContent(CARET_ID, "|")
        }
    }

    // Placeholder sized in em so the caret tracks the font metrics and scales with [size].
    val inlineContent = if (showCaret) {
        mapOf(
            CARET_ID to InlineTextContent(
                Placeholder(
                    width = 0.22.em,
                    height = 0.74.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                BlinkingCaret(color = mainColor)
            },
        )
    } else {
        emptyMap()
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            inlineContent = inlineContent,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

private const val CARET_ID = "caret"

/**
 * A thin vertical bar that hard-blinks (~530ms on / 530ms off, classic caret rate). Fills the
 * [Placeholder] bounds it is rendered into, so its height follows the surrounding text.
 */
@Composable
private fun BlinkingCaret(color: Color) {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1060
                1f at 0
                1f at 530
                0f at 531
                0f at 1060
            },
        ),
        label = "caretAlpha",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight()
                .graphicsLayer { this.alpha = alpha }
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    }
}
