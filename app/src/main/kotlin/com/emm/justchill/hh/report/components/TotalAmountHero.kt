package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Hero amount display for the Mes tab.
 *
 * "S/ " prefix and decimals (".00") are rendered in textTertiary at amountL size,
 * the integer part in textPrimary at amountL size. Uses AnnotatedString for mixed
 * color within a single Text — avoids baseline misalignment from multiple Text nodes.
 *
 * Example input: "S/ 6,200.00"
 */
@Composable
fun TotalAmountHero(totalFormatted: String, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    // Split "S/ 6,200.00" into ["S/ ", "6,200", ".00"]
    val annotated: AnnotatedString = buildAnnotatedString {
        val dotIndex = totalFormatted.lastIndexOf('.')
        val slashEnd = totalFormatted.indexOf(' ') + 1 // after "S/ "

        if (dotIndex < 0 || slashEnd <= 0) {
            // Fallback: render everything as primary
            withStyle(SpanStyle(color = colors.textPrimary)) {
                append(totalFormatted)
            }
        } else {
            // Currency prefix ("S/ ")
            withStyle(SpanStyle(color = colors.textTertiary, fontSize = type.amountL.fontSize)) {
                append(totalFormatted.substring(0, slashEnd))
            }
            // Integer part
            withStyle(SpanStyle(color = colors.textPrimary)) {
                append(totalFormatted.substring(slashEnd, dotIndex))
            }
            // Decimal part (".00")
            withStyle(SpanStyle(color = colors.textTertiary, fontSize = (type.amountL.fontSize.value * 0.6f).sp)) {
                append(totalFormatted.substring(dotIndex))
            }
        }
    }

    Text(
        text = annotated,
        style = type.amountL,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun TotalAmountHeroPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            TotalAmountHero(totalFormatted = "S/ 6,200.00")
        }
    }
}
