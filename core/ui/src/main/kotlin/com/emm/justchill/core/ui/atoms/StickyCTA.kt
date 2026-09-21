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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

// Every CTA shares this one height and no EmmSpacing step is 52dp, so it cannot become a token.
val CtaHeight: Dp = 52.dp

// A progress stroke is not a hairline; 2dp keeps the 16dp spinner legible and no EmmSpacing step is 2dp.
private val CtaSpinnerStroke: Dp = 2.dp

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
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

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
                .padding(horizontal = spacing.s4)
                .padding(top = spacing.s3, bottom = spacing.s4)
                .height(CtaHeight)
                .clip(radii.rL)
                .background(bgColor)
                .clickable(enabled = interactive, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (interaction == CtaInteraction.Loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(spacing.s4),
                        color = fgColor,
                        strokeWidth = CtaSpinnerStroke,
                    )
                    CtaLabel(text = label, color = fgColor)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CtaLabel(text = label, color = fgColor)
                    if (sublabel != null) {
                        Text(
                            text = sublabel,
                            style = type.caption,
                            color = fgColor.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CtaLabel(text: String, color: Color) {
    Text(text = text, style = LocalEmmType.current.titleM, color = color)
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun StickyCTAPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        Column(modifier = Modifier.background(colors.bg)) {
            StickyCTA(label = "Guardar", onClick = {}, sublabel = "S/ 120.00")
            StickyCTA(label = "Guardar", onClick = {}, interaction = CtaInteraction.Disabled)
        }
    }
}
