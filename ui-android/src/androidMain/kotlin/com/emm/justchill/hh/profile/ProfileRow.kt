package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow

@Composable
internal fun SectionHeader(text: String) {
    val spacing = LocalEmmSpacing.current
    Eyebrow(
        text = text,
        modifier = Modifier.padding(
            start = spacing.s5,
            end = spacing.s5,
            top = spacing.s5,
            bottom = spacing.s2,
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
internal fun HairlineDivider() {
    val colors = LocalEmmColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border),
    )
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
internal fun ChevronTrailing(enabled: Boolean) {
    val colors = LocalEmmColors.current
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = if (enabled) colors.textTertiary else colors.textDisabled,
        modifier = Modifier.size(16.dp),
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
            .padding(horizontal = spacing.s5, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTileSmall(icon = icon, tint = if (enabled) colors.textSecondary else colors.textDisabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = type.bodyL.copy(fontWeight = FontWeight.W500),
                color = if (enabled) colors.textPrimary else colors.textDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.W400,
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
private fun IconTileSmall(icon: ImageVector, tint: Color = LocalEmmColors.current.textSecondary) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(radii.rXS)
            .background(colors.surface2),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
    }
}
