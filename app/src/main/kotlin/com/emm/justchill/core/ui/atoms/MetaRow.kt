package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

/**
 * A single key-value row (e.g. "Category · Transport", "Date · May 15").
 *
 * Padding: 14dp vertical × 20dp horizontal. Optional bottom [Hairline] when [last] is false.
 *
 * @param label       Left-side label text.
 * @param value       Right-side value text.
 * @param icon        Optional leading icon before the label.
 * @param valueColor  Override color for the value; defaults to [EmmColors.textPrimary].
 * @param last        If true, suppresses the bottom hairline divider.
 * @param onClick     If non-null, the row becomes clickable and shows a chevron at the end.
 */
@Composable
fun MetaRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: Color? = null,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(modifier = rowModifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = label,
                    style = type.bodyM,
                    color = colors.textSecondary,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = type.bodyM,
                    color = valueColor ?: colors.textPrimary,
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        if (!last) {
            Hairline(insetStart = if (icon != null) 44.dp else 20.dp)
        }
    }
}
