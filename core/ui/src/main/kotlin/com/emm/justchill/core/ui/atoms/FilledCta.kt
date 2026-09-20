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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun FilledCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interaction: CtaInteraction = CtaInteraction.Enabled,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current

    val interactive = interaction == CtaInteraction.Enabled

    val bgColor = if (interactive) colors.textPrimary else colors.surface1
    val fgColor = if (interactive) colors.bg else colors.textTertiary

    Box(
        modifier = modifier
            .fillMaxWidth()
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
                Text(
                    text = label,
                    style = type.titleM,
                    color = fgColor,
                )
            }
        } else {
            Text(
                text = label,
                style = type.titleM,
                color = fgColor,
            )
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun FilledCtaPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            FilledCta(label = "Guardar", onClick = {})
            FilledCta(label = "Guardar", onClick = {}, interaction = CtaInteraction.Disabled)
        }
    }
}
