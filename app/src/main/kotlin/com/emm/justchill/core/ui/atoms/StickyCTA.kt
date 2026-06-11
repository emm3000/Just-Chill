package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii

/** Shared height for full-width CTA atoms (StickyCTA and FilledCta). */
internal val CtaHeight = 52.dp

/** Interaction state for full-width CTA atoms. */
enum class CtaInteraction {
    /** Button is active and clickable — normal accent visuals. */
    Enabled,

    /** Button is dimmed (surface1/textTertiary) and not clickable. */
    Disabled,

    /** Button is dimmed, not clickable, and shows a 16dp spinner before the label. */
    Loading,
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
 * @param interaction    Controls enabled/disabled/loading state. Default: [CtaInteraction.Enabled].
 */
@Composable
fun StickyCTA(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    inlineSublabel: Boolean = false,
    tone: CtaTone = CtaTone.Accent,
    interaction: CtaInteraction = CtaInteraction.Enabled,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    val interactive = interaction == CtaInteraction.Enabled

    val (bgColor, fgColor) = when {
        !interactive -> colors.surface1 to colors.textTertiary
        tone == CtaTone.Accent -> colors.accent to colors.textOnAccent
        tone == CtaTone.Pos -> colors.success to colors.textOnAccent
        else -> colors.textPrimary to colors.bg
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Hairline()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp)
                .height(CtaHeight)
                .clip(radii.rL)
                .background(bgColor)
                .then(
                    if (interactive) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (interaction == CtaInteraction.Loading) {
                // Loading state: spinner + label side by side, dimmed colours
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = fgColor,
                        strokeWidth = 2.dp,
                    )
                    CtaLabel(text = label, color = fgColor)
                }
            } else if (inlineSublabel && sublabel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CtaLabel(text = label, color = fgColor)
                    Text(
                        text = "·",
                        color = fgColor.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                    )
                    Text(
                        text = sublabel,
                        color = fgColor.copy(alpha = 0.9f),
                        style = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.W600,
                            letterSpacing = (-0.15).sp,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CtaLabel(text = label, color = fgColor, withLetterSpacing = false)
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

/**
 * Shared label style for StickyCTA content branches.
 *
 * Single-line labels use negative tracking ((-0.15).sp); the stacked-sublabel branch
 * intentionally renders without it ([withLetterSpacing] = false).
 */
@Composable
private fun CtaLabel(text: String, color: Color, withLetterSpacing: Boolean = true) {
    Text(
        text = text,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.W600,
        fontFamily = InterFontFamily,
        letterSpacing = if (withLetterSpacing) (-0.15).sp else 0.sp,
    )
}
