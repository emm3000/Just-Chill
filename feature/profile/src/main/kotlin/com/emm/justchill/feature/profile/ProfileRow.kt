package com.emm.justchill.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.atoms.ChevronTrailing
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

// No EmmSpacing step sits at 18dp; the glyph is about half the 40dp tile, same ladder as
// core/ui's IconTile.Lg (IconTile.kt's LG_GLYPH_SIZE).
private val TileGlyphSize: Dp = 18.dp

// No EmmSpacing step sits at 2dp; s1 doubles the label-to-meta gap, s0 removes it.
private val LabelMetaGap: Dp = 2.dp

@Composable
internal fun SectionHeader(text: String) {
    val spacing = LocalEmmSpacing.current
    Eyebrow(
        text = text,
        modifier = Modifier.padding(
            start = spacing.s6,
            end = spacing.s6,
            top = spacing.s6,
            bottom = spacing.s1,
        ),
    )
}

@Composable
internal fun ProfileGroup(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

@Composable
internal fun ProfileRow(icon: ImageVector, label: String, meta: String, metaIsPrimary: Boolean, onClick: () -> Unit) {
    ProfileRowWithTrailing(
        icon = icon,
        label = label,
        meta = meta,
        metaIsPrimary = metaIsPrimary,
        onClick = onClick,
        trailing = { ChevronTrailing(enabled = true) },
    )
}

@Composable
internal fun ProfileRowWithTrailing(
    icon: ImageVector,
    label: String,
    meta: String,
    metaIsPrimary: Boolean,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
    metaColor: Color? = null,
    enabled: Boolean = true,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg: Color = if (onClick != null && isPressed) colors.surface1 else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = spacing.s6, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        IconTile(icon = icon, tint = if (enabled) colors.textSecondary else colors.textTertiary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = type.bodyL.copy(fontWeight = FontWeight.W500),
                color = if (enabled) colors.textPrimary else colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(LabelMetaGap))
                Text(
                    text = meta,
                    style = type.bodyM,
                    color = if (enabled) {
                        metaColor ?: if (metaIsPrimary) colors.textSecondary else colors.textTertiary
                    } else {
                        colors.textDisabled
                    },
                )
            }
        }
        trailing()
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spacing.s10)
            .clip(radii.rM)
            .background(colors.surface1)
            .border(width = spacing.hairline, color = colors.border, shape = radii.rM),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(TileGlyphSize),
        )
    }
}
