package com.emm.justchill.hh.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone

/**
 * Always mounted, however empty the ledger is: this row is the only door to `LoansRoute` inside the
 * app, and gating a feature's entry point on that feature already having data is the landmine
 * `ui-android/CLAUDE.md` records.
 */
@Composable
internal fun LoansSection(totalOwed: String, people: List<String>, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Eyebrow(
            text = "Préstamos",
            modifier = Modifier.padding(start = spacing.s6, end = spacing.s6, top = spacing.s6, bottom = spacing.s2),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.s12)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Ver préstamos",
                    onClick = onClick,
                )
                .padding(horizontal = spacing.s6, vertical = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            IconTile(
                icon = Icons.Outlined.People,
                size = IconTileSize.Lg,
                tone = IconTileTone.Swatch,
                swatch = colors.success,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                Text(text = "Te deben", style = type.titleM, color = colors.textPrimary)
                Text(
                    text = loansSubtitle(people),
                    style = type.labelM,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = totalOwed,
                style = type.amountM,
                color = if (people.isEmpty()) colors.textTertiary else colors.success,
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(spacing.s4),
            )
        }
    }
}

internal fun loansSubtitle(people: List<String>): String = when (people.size) {
    0 -> "Nadie te debe"
    1 -> "1 persona · ${people.first()}"
    else -> "${people.size} personas · ${people.joinToString(", ")}"
}
