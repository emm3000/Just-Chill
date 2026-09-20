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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.ui.theme.InterFontFamily
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii

val CtaHeight = 52.dp

enum class CtaInteraction {
    Enabled,
    Disabled,
    Loading,
}

@Composable
fun StickyCTA(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    tone: CtaTone = CtaTone.Neutral,
    interaction: CtaInteraction = CtaInteraction.Enabled,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    val interactive = interaction == CtaInteraction.Enabled

    val (bgColor, fgColor) = when {
        !interactive -> colors.surface1 to colors.textTertiary
        tone == CtaTone.Pos -> colors.success to colors.bg
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
                .clickable(enabled = interactive, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (interaction == CtaInteraction.Loading) {
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
