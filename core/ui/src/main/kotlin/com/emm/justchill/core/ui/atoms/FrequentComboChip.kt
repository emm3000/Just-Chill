package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun FrequentComboChip(
    label: String,
    dotColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(spacing.s12)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .height(spacing.s8)
                .clip(radii.rFull)
                .border(spacing.hairline, if (active) colors.borderFocus else colors.border, radii.rFull)
                .indication(interactionSource, ripple())
                .padding(horizontal = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            if (dotColor != null) {
                CategoryDot(color = dotColor)
            }
            Text(
                text = label,
                style = type.labelM,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun FrequentComboChipPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            FrequentComboChip(label = "Comida", dotColor = colors.catOchre, onClick = {})
            FrequentComboChip(
                label = "Transporte",
                dotColor = colors.catSlate,
                onClick = {},
                active = true,
            )
        }
    }
}
