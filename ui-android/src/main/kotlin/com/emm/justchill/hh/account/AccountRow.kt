package com.emm.justchill.hh.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.EmmRowMenu
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconTileSize

// EmmRowMenu wraps an 18dp glyph in a 48dp target (DESIGN_SYSTEM.md §4); the row gives the surplus
// back at the screen edge so the glyph still sits on the 24dp column the header and the tiles use.
private val RowMenuEdgeGiveback: Dp = 15.dp

@Composable
internal fun AccountRow(row: AccountMonthUi, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.s6,
                    end = spacing.s6 - RowMenuEdgeGiveback,
                    top = spacing.s4,
                    bottom = spacing.s4,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            AccountIconTile(
                icon = row.account.type.toIcon(),
                tintColor = accountDotColor(row.account.name, colors),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                Text(text = row.account.name, style = type.titleM, color = colors.textPrimary)
                Text(
                    text = accountSubtitle(row.account.type.toLabel(), row.movementCount),
                    style = type.labelM,
                    color = colors.textTertiary,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                Text(
                    text = row.net,
                    style = type.amountM,
                    color = if (row.movementCount == 0) colors.textTertiary else colors.textPrimary,
                )
                Text(text = "este mes", style = type.caption, color = colors.textTertiary)
            }

            EmmRowMenu(contentDescription = "Opciones de cuenta", onEdit = onEdit, onDelete = onDelete)
        }
        Hairline()
    }
}

internal fun accountSubtitle(typeLabel: String, movementCount: Int): String = when (movementCount) {
    0 -> "Sin movimientos este mes"
    1 -> "$typeLabel · 1 movimiento"
    else -> "$typeLabel · $movementCount movimientos"
}

@Composable
private fun AccountIconTile(icon: ImageVector, tintColor: Color) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val tile = IconTileSize.Lg

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(tile.tileSize)
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(tile.iconSize),
        )
    }
}
