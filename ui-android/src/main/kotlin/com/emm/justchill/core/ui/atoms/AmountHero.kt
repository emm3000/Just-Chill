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
 * [showCaret] draws a caret that never moves: every caller that shows one feeds a cents
 * accumulator, where digits only ever enter at the right edge, so there is no other position for
 * it to be in.
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

    val prefixStyle = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = prefixSize,
    )

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
