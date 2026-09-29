package com.emm.justchill.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.atoms.CategoryDot
import com.emm.justchill.core.ui.category.IconCatalog
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun PreviewChip(
    name: String,
    icon: IconCatalog,
    colorId: String,
    type: CategoryType,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val emmType: EmmType = LocalEmmType.current

    val displayName: String = name.trim().ifBlank { "Tu categoría" }
    val nameColor: Color = if (name.isBlank()) colors.textTertiary else colors.textPrimary
    val shape: Shape = radii.rFull

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        modifier = modifier
            .clip(shape)
            .background(colors.surface1)
            .border(spacing.hairline, colors.border, shape)
            .padding(start = spacing.s2, end = spacing.s3, top = spacing.s2, bottom = spacing.s2),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(spacing.s8)
                .clip(radii.rS)
                .background(colors.surface3),
        ) {
            Icon(
                imageVector = icon.icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(spacing.s4),
            )
        }
        CategoryDot(color = colors.resolvedColor(colorId))
        Text(
            text = displayName,
            style = emmType.titleM,
            color = nameColor,
        )
        TypeBadge(type = type)
    }
}

@Composable
private fun TypeBadge(type: CategoryType) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val emmType: EmmType = LocalEmmType.current
    val isIncome: Boolean = type == CategoryType.Income
    val fg: Color = if (isIncome) colors.success else colors.textSecondary
    val label: String = if (isIncome) "Ingreso" else "Gasto"

    Text(
        text = label,
        modifier = Modifier
            .clip(radii.rFull)
            .background(colors.surface2)
            .padding(horizontal = spacing.s2, vertical = spacing.s1),
        style = emmType.caption.copy(fontWeight = FontWeight.W600),
        color = fg,
    )
}
