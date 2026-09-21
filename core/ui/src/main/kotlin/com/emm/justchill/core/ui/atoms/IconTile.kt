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
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

enum class IconTileSize { Sm, Md, Lg }

@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, size: IconTileSize = IconTileSize.Md) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val geometry: IconTileGeometry = when (size) {
        IconTileSize.Sm -> IconTileGeometry(spacing.s6, spacing.s3, RoundedCornerShape(SM_TILE_RADIUS))
        IconTileSize.Md -> IconTileGeometry(spacing.s8, MD_GLYPH_SIZE, RoundedCornerShape(MD_TILE_RADIUS))
        IconTileSize.Lg -> IconTileGeometry(spacing.s10, LG_GLYPH_SIZE, LocalEmmRadii.current.rM)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(geometry.tileSize)
            .clip(geometry.shape)
            .border(spacing.hairline, colors.border, geometry.shape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(geometry.glyphSize),
        )
    }
}

private data class IconTileGeometry(val tileSize: Dp, val glyphSize: Dp, val shape: Shape)

// Every tile rounds at about 0.3 of its side, as Lg's rM does on 40dp; no EmmRadii step is 7dp or 9dp.
private val SM_TILE_RADIUS: Dp = 7.dp
private val MD_TILE_RADIUS: Dp = 9.dp

// Every glyph is about half its tile, as Sm's s3 is on 24dp; no EmmSpacing step is 15dp or 18dp.
private val MD_GLYPH_SIZE: Dp = 15.dp
private val LG_GLYPH_SIZE: Dp = 18.dp

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
