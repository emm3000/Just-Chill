package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

@Composable
fun BackBtn(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    IconBtn(
        icon = Icons.Outlined.ChevronLeft,
        onClick = onClick,
        contentDescription = "Volver",
        modifier = modifier,
        glyphSize = spacing.s6,
    )
}

@Preview
@Composable
private fun BackBtnPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Box(
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            BackBtn(onClick = {})
        }
    }
}
