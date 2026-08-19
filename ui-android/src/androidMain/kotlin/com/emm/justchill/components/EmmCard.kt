package com.emm.justchill.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing

@Composable
fun EmmCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(radii.rM)
            .background(colors.surface1)
            .border(BorderStroke(1.dp, colors.border), radii.rM)
            .padding(spacing.s4),
    ) {
        content()
    }
}
