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

@Composable
fun OutlinedCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interaction: CtaInteraction = CtaInteraction.Enabled,
    leading: (@Composable () -> Unit)? = null,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (interaction == CtaInteraction.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = textColor,
                    strokeWidth = 2.dp,
                )
            } else if (leading != null) {
                leading()
            }
            Text(
                text = label,
                style = type.titleM,
                color = textColor,
            )
        }
    }
}
