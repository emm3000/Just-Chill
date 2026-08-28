package com.emm.justchill.hh.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

// Both segments hold the same width so the pill keeps its size and its centre when the selection
// moves; sizing each to its own label would shift "Ingreso"/"Gasto" sideways on every tap.
private val SignSegmentWidth: Dp = 96.dp

/**
 * Selection reads through the text ladder and one surface step — never a tint. An expense is not
 * red and an income is not green here (`DESIGN_SYSTEM.md` §1.4); the amount above carries that.
 */
@Composable
internal fun SignToggle(
    isSpend: Boolean,
    onIncomeClick: () -> Unit,
    onSpendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = modifier
            .height(spacing.s12)
            .clip(radii.rFull)
            .background(colors.bg)
            .border(1.dp, colors.border, radii.rFull),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignSegment(label = "Ingreso", selected = !isSpend, onClick = onIncomeClick)
        SignSegment(label = "Gasto", selected = isSpend, onClick = onSpendClick)
    }
}

@Composable
private fun SignSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    // The target is the whole segment; the fill sits inset inside it, so a tap on the pill's edge
    // still selects (DESIGN_SYSTEM.md §4). Selection is a surface step and a weight — neither
    // reaches TalkBack, so it is stated.
    Box(
        modifier = Modifier
            .width(SignSegmentWidth)
            .fillMaxHeight()
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.s1)
                .clip(radii.rFull)
                .background(if (selected) colors.surface2 else Color.Transparent),
        ) {
            Text(
                text = label,
                style = type.labelL,
                fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
                color = if (selected) colors.textPrimary else colors.textTertiary,
            )
        }
    }
}

// The two chips share a row and not its width: a category name ("Supermercado") is the longest
// label the form carries, an account is a bank's four letters.
internal const val ACCOUNT_CHIP_WEIGHT = 1f
internal const val CATEGORY_CHIP_WEIGHT = 1.6f

@Composable
internal fun SelectorChip(
    label: String,
    dotColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector = Icons.Outlined.KeyboardArrowDown,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(spacing.s12)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.s10)
                .clip(radii.rFull)
                .background(colors.surface1)
                .border(1.dp, colors.border, radii.rFull)
                .padding(horizontal = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            if (dotColor != null) {
                ChipDot(color = dotColor)
            }
            Text(
                text = label,
                style = type.labelL,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(spacing.s4),
            )
        }
    }
}

@Composable
internal fun FrequentComboChip(
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

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(spacing.s12)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .height(spacing.s8)
                .clip(radii.rFull)
                .background(if (active) colors.surface2 else colors.surface1)
                .border(1.dp, if (active) colors.borderFocus else colors.border, radii.rFull)
                .padding(horizontal = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            if (dotColor != null) {
                ChipDot(color = dotColor)
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

@Composable
private fun ChipDot(color: Color) {
    val spacing = LocalEmmSpacing.current

    Box(
        modifier = Modifier
            .size(spacing.s2)
            .clip(CircleShape)
            .background(color),
    )
}
