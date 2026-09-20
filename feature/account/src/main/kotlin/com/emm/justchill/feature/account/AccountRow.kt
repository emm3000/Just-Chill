package com.emm.justchill.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.EmmRowMenu
import com.emm.justchill.core.ui.atoms.EmmRowMenuGlyphSize
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.color
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.core.ui.theme.edgeGiveback

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
                    end = spacing.s6 - spacing.edgeGiveback(EmmRowMenuGlyphSize),
                    top = spacing.s4,
                    bottom = spacing.s4,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            IconTile(icon = row.account.type.toIcon(), size = IconTileSize.Lg)

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
                    color = accountNetTone(row.movementCount, row.netIsPositive).color(colors),
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

/**
 * A positive net is positive money — `success` — but only once the account has activity; a
 * silent account keeps its muted step regardless of what its empty net would sign.
 */
internal fun accountNetTone(movementCount: Int, netIsPositive: Boolean): AmountTone = when {
    movementCount == 0 -> AmountTone.Mute
    netIsPositive -> AmountTone.Pos
    else -> AmountTone.Neutral
}
