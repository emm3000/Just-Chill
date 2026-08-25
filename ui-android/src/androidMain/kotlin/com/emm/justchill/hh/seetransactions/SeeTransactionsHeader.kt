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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

@Composable
internal fun ScreenHeader(isCategoryFilterActive: Boolean, onSearch: () -> Unit, onFilter: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 14.dp),
    ) {
        Text(
            text = "Transacciones",
            style = type.headlineM.copy(fontSize = 22.sp, letterSpacing = (-0.44).sp),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        HeaderAction(
            icon = Icons.Outlined.Search,
            contentDescription = "Buscar transacciones",
            onClick = onSearch,
        )
        Spacer(Modifier.width(8.dp))
        HeaderAction(
            icon = Icons.Outlined.FilterList,
            contentDescription = if (isCategoryFilterActive) {
                "Filtrar por categoría, filtro activo"
            } else {
                "Filtrar por categoría"
            },
            onClick = onFilter,
            showBadge = isCategoryFilterActive,
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
    val shape = RoundedCornerShape(12.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(colors.surface1)
            .border(1.dp, colors.border, shape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        if (showBadge) {
            // The surface1 ring keeps the accent dot legible where the badge overlaps the icon.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 9.dp)
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
