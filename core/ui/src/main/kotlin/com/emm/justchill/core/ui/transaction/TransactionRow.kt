package com.emm.justchill.core.ui.transaction

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.atoms.CategoryDotSlot
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.category.resolvedIcon
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.InterFontFamily
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun TransactionRow(tx: TransactionUi, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    val amountColor: Color = if (tx.type == TransactionType.Income) colors.success else colors.textPrimary
    val dotColor: Color = colors.resolvedColor(tx.category.colorId)

    val baseModifier: Modifier = modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = spacing.s6, vertical = spacing.s2)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        IconTile(icon = tx.category.resolvedIcon, size = IconTileSize.Lg)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                CategoryDotSlot(color = dotColor.takeIf { tx.categoryLeadsTitle })
                Text(
                    text = tx.title,
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
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                CategoryDotSlot(color = dotColor.takeIf { !tx.categoryLeadsTitle })
                Text(
                    text = tx.subtitle,
                    style = type.caption.copy(fontSize = 12.sp, letterSpacing = 0.sp),
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
