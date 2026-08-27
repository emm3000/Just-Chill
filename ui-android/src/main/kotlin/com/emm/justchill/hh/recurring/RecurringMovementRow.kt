package com.emm.justchill.hh.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.EmmRowMenu
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.hh.category.findById

@Composable
private fun DayBadge(dayOfMonth: Int) {
    val colors = LocalEmmColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface2)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
    ) {
        Text(
            text = dayOfMonth.toString().padStart(2, '0'),
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            letterSpacing = (-0.1).sp,
        )
        Text(
            text = "DEL MES",
            fontSize = 9.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = colors.textTertiary,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
internal fun RecurringMovementRow(
    item: RecurringMovementUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    trailingBadge: (@Composable () -> Unit)? = null,
) {
    val colors = LocalEmmColors.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DayBadge(item.dayOfMonth)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.15).sp,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.categoryColor?.let { colorKey ->
                        val dotColor = remember(colorKey) { findById(colorKey).primary }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${item.categoryName} · ${item.accountName}",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            color = colors.textTertiary,
                        )
                    } ?: run {
                        Text(
                            text = item.accountName,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            color = colors.textTertiary,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.isVariableAmount) {
                    Text(
                        text = "Variable",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        fontStyle = FontStyle.Italic,
                        color = colors.textTertiary,
                    )
                } else {
                    val amountColor = if (item.type == TransactionType.Income) colors.success else colors.textPrimary
                    Text(
                        text = item.formattedAmount,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                        color = amountColor,
                        letterSpacing = (-0.1).sp,
                    )
                }
                trailingBadge?.let {
                    Spacer(Modifier.height(3.dp))
                    it()
                }
            }

            EmmRowMenu(contentDescription = "Opciones", onEdit = onEdit, onDelete = onDelete)
        }
        Hairline()
    }
}
