package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

@Composable
fun MonthChevron(
    direction: MonthChevronDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = direction.contentDescription,
) {
    val colors: EmmColors = LocalEmmColors.current
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(spacing.s12)
            .clip(radii.rFull)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = direction.icon,
            contentDescription = contentDescription,
            tint = colors.textSecondary,
            modifier = Modifier.size(spacing.s5),
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun MonthChevronPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            MonthChevron(direction = MonthChevronDirection.Previous, onClick = {})
            MonthChevron(direction = MonthChevronDirection.Next, onClick = {})
        }
    }
}
