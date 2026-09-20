package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

enum class IconTileSize(val tileSize: Dp, val iconSize: Dp, val radius: Dp) {
    Sm(24.dp, 12.dp, 7.dp),
    Md(32.dp, 15.dp, 9.dp),
    Lg(40.dp, 18.dp, 12.dp),
}

@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, size: IconTileSize = IconTileSize.Md) {
    val colors: EmmColors = LocalEmmColors.current
    val shape: Shape = RoundedCornerShape(size.radius)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.tileSize)
            .clip(shape)
            .border(1.dp, colors.border, shape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(size.iconSize),
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun IconTilePreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            IconTile(icon = Icons.Outlined.ShoppingCart, size = IconTileSize.Sm)
            IconTile(icon = Icons.Outlined.ShoppingCart, size = IconTileSize.Md)
            IconTile(icon = Icons.Outlined.ShoppingCart, size = IconTileSize.Lg)
        }
    }
}
