package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

@Composable
fun SheetDragHandle(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacing.s2, bottom = spacing.s1),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = spacing.s8, height = spacing.s1)
                .background(colors.borderFocus, radii.rFull),
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun SheetDragHandlePreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Box(
            modifier = Modifier
                .background(colors.surface1)
                .padding(vertical = spacing.s2),
        ) {
            SheetDragHandle()
        }
    }
}
