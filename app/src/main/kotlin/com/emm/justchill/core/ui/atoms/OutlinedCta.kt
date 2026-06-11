package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * @param label   Button label.
 * @param enabled When false, the button is dimmed and non-interactive. The node remains in the
 *                semantics tree so assistive technologies can announce it as disabled.
 * @param onClick Action fired on tap (only when [enabled]).
 */
@Composable
fun OutlinedCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current

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
        Text(
            text = label,
            style = type.titleM,
            color = textColor,
        )
    }
}
