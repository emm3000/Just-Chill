package com.emm.justchill.feature.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

@Composable
internal fun ScreenHeader(isCategoryOrAmountFilterActive: Boolean, onIntent: (SeeTransactionsIntent) -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    JcTopBar(
        title = "Movimientos",
        right = {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                HeaderAction(
                    icon = Icons.Outlined.Search,
                    contentDescription = "Buscar transacciones",
                    onClick = { onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnSearchRequested) },
                )
                HeaderAction(
                    icon = Icons.Outlined.FilterList,
                    contentDescription = if (isCategoryOrAmountFilterActive) {
                        "Filtrar movimientos, filtro activo"
                    } else {
                        "Filtrar movimientos"
                    },
                    onClick = { onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnFilterSheetRequested) },
                    showBadge = isCategoryOrAmountFilterActive,
                )
            }
        },
        column = spacing.s6,
    )
}

@Composable
internal fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    showBadge: Boolean = false,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spacing.s12)
            .clip(radii.rM)
            .background(colors.surface1)
            .border(spacing.hairline, colors.border, radii.rM)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(spacing.s5),
        )
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = spacing.s2, end = spacing.s2)
                    .size(spacing.s2)
                    .clip(CircleShape)
                    .background(colors.surface1)
                    .padding(spacing.hairline)
                    .clip(CircleShape)
                    .background(colors.textPrimary),
            )
        }
    }
}
