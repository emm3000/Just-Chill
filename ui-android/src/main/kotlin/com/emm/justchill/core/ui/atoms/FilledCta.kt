package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmType

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

    val bgColor = if (interactive) colors.accent else colors.surface1
    val fgColor = if (interactive) colors.textOnAccent else colors.textTertiary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CtaHeight)
            .clip(radii.rL)
            .background(bgColor)
            .then(
                if (interactive) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .semantics { role = Role.Button },
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
