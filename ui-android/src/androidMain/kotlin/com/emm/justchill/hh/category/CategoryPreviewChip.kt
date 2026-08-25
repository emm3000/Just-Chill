package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.category.CategoryType
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors

@Composable
internal fun PreviewChip(
    name: String,
    icon: IconCatalog,
    color: CategoryColor,
    type: CategoryType,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    val displayName = name.trim().ifBlank { "Tu categoría" }
    val nameColor = if (name.isBlank()) colors.textTertiary else colors.textPrimary
    val shape = RoundedCornerShape(999.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(shape)
            .background(colors.surface1)
            .border(1.dp, colors.border, shape)
            .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface3),
        ) {
            Icon(
                imageVector = icon.icon,
                contentDescription = null,
                tint = color.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = nameColor,
            letterSpacing = (-0.15).sp,
        )
        TypeBadge(type = type)
    }
}

@Composable
private fun TypeBadge(type: CategoryType) {
    val colors = LocalEmmColors.current
    val isIncome = type == CategoryType.Income
    val bg = if (isIncome) colors.posMuted else colors.negMuted
    val fg = if (isIncome) colors.success else colors.danger
    val label = if (isIncome) "Ingreso" else "Gasto"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = fg,
            letterSpacing = 0.sp,
        )
    }
}
