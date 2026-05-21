package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Full-width secondary outlined button "Compartir reporte" with IosShare icon.
 * Variant: Button.Secondary (§7.1 — surface1 bg, textPrimary, 1dp border).
 */
@Composable
fun ShareReportButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val shape = RoundedCornerShape(6.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(colors.surface1)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = spacing.s5),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.IosShare,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.size(spacing.s2))
        Text(
            text = "Compartir reporte",
            style = type.labelL,
            color = colors.textPrimary,
        )
    }
}

@PreviewLightDark
@Composable
private fun ShareReportButtonPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            ShareReportButton(onClick = {})
        }
    }
}
