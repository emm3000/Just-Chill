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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun OutlinedCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interaction: CtaInteraction = CtaInteraction.Enabled,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors: EmmColors = LocalEmmColors.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    val enabled: Boolean = interaction == CtaInteraction.Enabled
    val loading: Boolean = interaction == CtaInteraction.Loading
    val textColor: Color = if (enabled) colors.textPrimary else colors.textDisabled

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
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(spacing.s4),
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
            if (!loading && trailing != null) {
                trailing()
            }
        }
    }
}
