package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.shared.monthLabel

private val CHEVRON_TAP_TARGET = 32.dp
private val ACTION_TAP_TARGET = 40.dp

/**
 * The browsed month IS the screen title. A filtered list crosses months, so the title and its
 * arrows step aside there rather than heading rows they no longer govern.
 */
@Composable
internal fun ScreenHeader(
    state: SeeTransactionsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSearch: () -> Unit,
    onFilter: () -> Unit,
) {
    val spacing = LocalEmmSpacing.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.s3, start = spacing.s6, end = spacing.s6),
    ) {
        if (state.isMonthSelectorVisible) {
            MonthTitle(
                month = state.month,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        HeaderAction(
            icon = Icons.Outlined.Search,
            contentDescription = "Buscar transacciones",
            onClick = onSearch,
        )
        Spacer(Modifier.width(spacing.s2))
        HeaderAction(
            icon = Icons.Outlined.FilterList,
            contentDescription = if (state.activeCategory != null) {
                "Filtrar por categoría, filtro activo"
            } else {
                "Filtrar por categoría"
            },
            onClick = onFilter,
            showBadge = state.activeCategory != null,
        )
    }
}

@Composable
private fun MonthTitle(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        MonthChevron(
            icon = Icons.Outlined.ChevronLeft,
            contentDescription = "Mes anterior",
            onClick = onPreviousMonth,
        )
        // "Septiembre 2026" at headlineL does not fit beside both header actions on a phone;
        // the step down keeps the month whole rather than ellipsising the year off it.
        BasicText(
            text = buildAnnotatedString {
                append(month.monthLabel())
                withStyle(SpanStyle(color = colors.textTertiary, fontWeight = FontWeight.W500)) {
                    append(" ${month.year}")
                }
            },
            style = type.headlineL.copy(color = colors.textPrimary),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
                minFontSize = type.titleL.fontSize,
                maxFontSize = type.headlineL.fontSize,
            ),
            modifier = Modifier.weight(1f, fill = false),
        )
        MonthChevron(
            icon = Icons.Outlined.ChevronRight,
            contentDescription = "Mes siguiente",
            onClick = onNextMonth,
        )
    }
}

@Composable
private fun MonthChevron(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = LocalEmmColors.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(CHEVRON_TAP_TARGET)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textSecondary,
            modifier = Modifier.size(LocalEmmSpacing.current.s5),
        )
    }
}

@Composable
internal fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    showBadge: Boolean = false,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ACTION_TAP_TARGET)
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(LocalEmmSpacing.current.s5),
        )
        if (showBadge) {
            // The surface1 ring keeps the accent dot legible where the badge overlaps the icon.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 7.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(colors.surface1)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
    }
}
