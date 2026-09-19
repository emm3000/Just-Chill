package com.emm.justchill.feature.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

@Composable
internal fun LoanChevronTrailing() {
    val size: Dp = LocalEmmSpacing.current.s4
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = LocalEmmColors.current.textTertiary,
        modifier = Modifier.size(size),
    )
}

@Preview
@Composable
private fun LoanChevronTrailingPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .background(LocalEmmColors.current.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            LoanChevronTrailing()
        }
    }
}
