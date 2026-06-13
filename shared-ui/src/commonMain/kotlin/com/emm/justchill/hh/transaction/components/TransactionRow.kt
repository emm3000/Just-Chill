package com.emm.justchill.hh.transaction.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone
import com.emm.justchill.hh.transaction.TransactionUi

@Composable
fun TransactionRow(tx: TransactionUi, showDate: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val amountColor = if (tx.type == TransactionType.Income) colors.success else colors.textPrimary

    val subtitle = if (showDate) "${tx.readableDate} · ${tx.readableTime}" else tx.readableTime

    val baseModifier = modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 24.dp, vertical = 12.dp)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(
            icon = tx.category.categoryIcon,
            size = IconTileSize.Sm,
            tone = IconTileTone.Swatch,
            swatch = tx.category.categoryColor.primary,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifBlank { "Sin descripción" },
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.W500,
                    letterSpacing = (-0.15).sp,
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = type.caption.copy(fontSize = 12.sp, letterSpacing = 0.sp),
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = tx.amount,
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = (-0.15).sp,
                fontFeatureSettings = "tnum",
            ),
            color = amountColor,
        )
    }
}
