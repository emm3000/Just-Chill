package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.InterFontFamily
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun MonthSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
) {
    if (onLabelClick == null) {
        PlainMonthSelector(label = label, onPrevious = onPrevious, onNext = onNext, modifier = modifier)
    } else {
        PickerMonthSelector(
            label = label,
            onPrevious = onPrevious,
            onNext = onNext,
            onLabelClick = onLabelClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlainMonthSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(radii.rFull)
            .border(BorderStroke(1.dp, colors.border), radii.rFull)
            .padding(horizontal = 4.dp),
    ) {
        MonthChevron(direction = MonthChevronDirection.Previous, onClick = onPrevious)

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )

        MonthChevron(direction = MonthChevronDirection.Next, onClick = onNext)
    }
}

@Composable
private fun PickerMonthSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLabelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .height(spacing.s12)
            .clip(shape)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .padding(horizontal = spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        MonthChevron(direction = MonthChevronDirection.Previous, onClick = onPrevious)
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clip(shape)
                .clickable(onClick = onLabelClick)
                .padding(horizontal = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = type.labelL,
                color = colors.textPrimary,
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
        }
        MonthChevron(direction = MonthChevronDirection.Next, onClick = onNext)
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun MonthSelectorPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            MonthSelector(label = "Marzo 2026", onPrevious = {}, onNext = {})
            MonthSelector(label = "Marzo 2026", onPrevious = {}, onNext = {}, onLabelClick = {})
        }
    }
}
