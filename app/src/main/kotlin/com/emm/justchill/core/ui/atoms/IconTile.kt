package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors

enum class IconTileSize(val tileSize: Dp, val iconSize: Dp, val radius: Dp) {
    Sm(24.dp, 12.dp, 7.dp),
    Md(32.dp, 15.dp, 9.dp),
    Lg(40.dp, 18.dp, 12.dp),
}

enum class IconTileTone {
    Neutral, // surface2 bg + textTertiary icon
    Accent, // accentMuted bg + accent icon
    Swatch, // custom color (pass via swatch param, alpha ~14% bg)
}

/**
 * Rounded icon tile used for category icons in lists.
 *
 * @param swatch  Only used when [tone] is [IconTileTone.Swatch]. The icon is rendered
 *                in [swatch]; the background is [swatch] at ~14% opacity (0x24 alpha).
 */
@Composable
fun IconTile(
    icon: ImageVector,
    size: IconTileSize = IconTileSize.Md,
    tone: IconTileTone = IconTileTone.Neutral,
    swatch: Color? = null,
) {
    val colors = LocalEmmColors.current

    val bgColor: Color = when (tone) {
        IconTileTone.Neutral -> colors.surface2
        IconTileTone.Accent -> colors.accentMuted
        IconTileTone.Swatch -> swatch?.copy(alpha = 0.14f) ?: colors.surface2
    }
    val iconColor: Color = when (tone) {
        IconTileTone.Neutral -> colors.textTertiary
        IconTileTone.Accent -> colors.accent
        IconTileTone.Swatch -> swatch ?: colors.textTertiary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.tileSize)
            .clip(RoundedCornerShape(size.radius))
            .background(bgColor),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size.iconSize),
        )
    }
}
