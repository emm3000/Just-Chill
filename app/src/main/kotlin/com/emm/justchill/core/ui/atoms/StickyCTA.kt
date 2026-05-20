package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii

enum class CtaTone {
    Accent,   // bg = accent, fg = white
    Pos,      // bg = success, fg = white
    Neutral,  // bg = textPrimary, fg = bg
}

/**
 * Full-width sticky CTA button, typically pinned to the bottom of a screen.
 *
 * - Height: 52dp
 * - Radius: [EmmRadii.rL] (14dp)
 * - Bottom container padding: 12dp top + 16dp bottom  (caller should add WindowInsets padding above this)
 * - Top hairline: always present
 *
 * @param label          Primary button label.
 * @param sublabel       Optional secondary content.
 * @param inlineSublabel When true, renders `label · sublabel` in a single Row instead of
 *                       stacking them. Useful for the Add-Transaction CTA ("Anotar gasto · S/ 85.40").
 * @param enabled        When false, the button is dimmed and non-interactive.
 */
@Composable
fun StickyCTA(
    label: String,
    sublabel: String? = null,
    inlineSublabel: Boolean = false,
    tone: CtaTone = CtaTone.Accent,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    val ctaColors = when (tone) {
        CtaTone.Accent -> colors.accent to colors.textOnAccent
        CtaTone.Pos -> colors.success to colors.textOnAccent
        CtaTone.Neutral -> colors.textPrimary to colors.bg
    }
    val (bgColor, fgColor) = ctaColors

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Hairline()

        Surface(
            color = bgColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp)
                .height(52.dp)
                .clip(radii.rL)
                .alpha(if (enabled) 1f else 0.4f)
                .then(
                    if (enabled) Modifier.clickable(onClick = onClick)
                    else Modifier
                ),
        ) {
            if (inlineSublabel && sublabel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = label,
                        color = fgColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                        letterSpacing = (-0.07).sp,
                    )
                    Text(
                        text = "·",
                        color = fgColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                    )
                    Text(
                        text = sublabel,
                        color = fgColor.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = com.emm.justchill.core.theme.PlexMonoFontFamily,
                        letterSpacing = 0.sp,
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        color = fgColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                    )
                    if (sublabel != null) {
                        Text(
                            text = sublabel,
                            color = fgColor.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W400,
                            fontFamily = InterFontFamily,
                        )
                    }
                }
            }
        }
    }
}
