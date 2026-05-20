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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.PlexMonoFontFamily

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

    // Disabled state matches the design: bg → surface1 (panel), fg → textTertiary.
    val (bgColor, fgColor) = when {
        !enabled -> colors.surface1 to colors.textTertiary
        tone == CtaTone.Accent -> colors.accent to colors.textOnAccent
        tone == CtaTone.Pos -> colors.success to colors.textOnAccent
        else -> colors.textPrimary to colors.bg
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Hairline()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp)
                .height(52.dp)
                .clip(radii.rL)
                .background(bgColor)
                .then(
                    if (enabled) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
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
                        fontFamily = PlexMonoFontFamily,
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
