package com.emm.justchill.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.atoms.ChevronTrailing
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

// No EmmSpacing step sits at 2dp; s1 doubles the label-to-meta gap, s0 removes it.
private val LabelMetaGap: Dp = 2.dp

@Composable
internal fun SectionHeader(text: String) {
    val spacing: EmmSpacing = LocalEmmSpacing.current
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
    busy: Boolean = false,
) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    val active: Boolean = enabled || busy
    val press: Modifier = if (onClick != null) {
        pressableRow(onClick = onClick, enabled = enabled, pressedGround = colors.surface1)
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(press)
            .padding(horizontal = spacing.s6, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        IconTile(icon = icon, size = IconTileSize.Lg, enabled = active)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = type.bodyL.copy(fontWeight = FontWeight.W500),
                color = if (active) colors.textPrimary else colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(LabelMetaGap))
                Text(
                    text = meta,
                    style = type.bodyM,
                    color = if (active) {
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
private fun pressableRow(onClick: () -> Unit, enabled: Boolean, pressedGround: Color): Modifier {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val bg: Color = if (isPressed) pressedGround else Color.Transparent
    return Modifier
        .background(bg)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
}
