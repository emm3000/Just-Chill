package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Full-width outlined (bordered) secondary CTA.
 *
 * Mirrors [StickyCTA] height ([CtaHeight]) and shape ([EmmRadii.rL]).
 * Used for secondary actions that should not compete visually with the primary [StickyCTA].
 *
 * @param label       Button label.
 * @param interaction Controls enabled/disabled/loading state. Default: [CtaInteraction.Enabled].
 *                    [CtaInteraction.Loading] shows a spinner before the label; the button is dimmed
 *                    and non-interactive. [CtaInteraction.Disabled] dims the button without a spinner.
 * @param onClick     Action fired on tap (only when [CtaInteraction.Enabled]).
 */
@Composable
fun OutlinedCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interaction: CtaInteraction = CtaInteraction.Enabled,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current

    val enabled = interaction == CtaInteraction.Enabled
    val textColor = if (enabled) colors.textPrimary else colors.textDisabled

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CtaHeight)
            .clip(radii.rL)
            .border(width = 1.dp, color = colors.border, shape = radii.rL)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (interaction == CtaInteraction.Loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = textColor,
                    strokeWidth = 2.dp,
                )
                Text(
                    text = label,
                    style = type.titleM,
                    color = textColor,
                )
            }
        } else {
            Text(
                text = label,
                style = type.titleM,
                color = textColor,
            )
        }
    }
}
