package com.emm.justchill.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun ShareReportButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val shape: RoundedCornerShape = LocalEmmRadii.current.rXS

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.s12)
            .clip(shape)
            .background(colors.surface1)
            .border(width = spacing.hairline, color = colors.border, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.s5),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.IosShare,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(spacing.s5),
        )
        Box(modifier = Modifier.size(spacing.s2))
        Text(
            text = "Compartir reporte",
            style = type.labelL,
            color = colors.textPrimary,
        )
    }
}

@Preview
@Composable
private fun ShareReportButtonPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            ShareReportButton(onClick = {})
        }
    }
}
