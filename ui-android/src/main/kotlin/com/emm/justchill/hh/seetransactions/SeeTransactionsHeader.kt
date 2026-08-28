package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.theme.EmmSpacing
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.shared.monthLabel

/**
 * The browsed month IS the screen title. A filtered list crosses months, so [month] arrives `null`
 * there and the title steps aside rather than heading rows it no longer governs.
 */
@Composable
internal fun ScreenHeader(
    month: YearMonth?,
    isCategoryFilterActive: Boolean,
    onIntent: (SeeTransactionsIntent) -> Unit,
) {
    val spacing = LocalEmmSpacing.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = spacing.s3,
                start = spacing.s6 - spacing.headerEdgeGiveback,
                end = spacing.s6 - spacing.headerEdgeGiveback,
            ),
    ) {
        if (month != null) {
            MonthTitle(month = month, onIntent = onIntent, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        HeaderAction(
            icon = Icons.Outlined.Search,
            contentDescription = "Buscar transacciones",
            onClick = { onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnSearchRequested) },
        )
        Spacer(Modifier.width(spacing.s2))
        HeaderAction(
            icon = Icons.Outlined.FilterList,
            contentDescription = if (isCategoryFilterActive) {
                "Filtrar por categoría, filtro activo"
            } else {
                "Filtrar por categoría"
            },
            onClick = { onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnFilterSheetRequested) },
            showBadge = isCategoryFilterActive,
        )
    }
}

@Composable
private fun MonthTitle(month: YearMonth, onIntent: (SeeTransactionsIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        MonthChevron(
            icon = Icons.Outlined.ChevronLeft,
            contentDescription = "Mes anterior",
            onClick = { onIntent(SeeTransactionsIntent.OnPreviousMonth) },
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
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(
                minFontSize = type.titleL.fontSize,
                maxFontSize = type.headlineL.fontSize,
            ),
            modifier = Modifier.weight(1f, fill = false),
        )
        MonthChevron(
            icon = Icons.Outlined.ChevronRight,
            contentDescription = "Mes siguiente",
            onClick = { onIntent(SeeTransactionsIntent.OnNextMonth) },
        )
    }
}

@Composable
private fun MonthChevron(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spacing.s12)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textSecondary,
            modifier = Modifier.size(spacing.s5),
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
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spacing.s12)
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(spacing.s5),
        )
        if (showBadge) {
            // The surface1 ring keeps the accent dot legible where the badge overlaps the icon.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = spacing.s2, end = spacing.s2)
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

// A 48dp target (DESIGN_SYSTEM.md §4) wraps its 20dp glyph in 14dp of nothing. The header rows
// give that back at the screen edge, so the icons still sit on the 24dp column the list rows use.
internal val EmmSpacing.headerEdgeGiveback: Dp
    get() = (s12 - s5) / 2
