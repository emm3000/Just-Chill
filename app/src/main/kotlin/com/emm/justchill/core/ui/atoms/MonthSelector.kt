package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii

/**
 * Month navigation control — prev/next arrows flanking a centered label.
 *
 * Arrow zones: 36dp × 36dp, rounded [EmmRadii.rFull].
 * Outer pill: surface1 background + border, 4dp vertical padding.
 * Label: 13sp w600 Inter.
 *
 * @param label   Formatted month string e.g. "Mayo 2026".
 * @param onPrev  Called when the left chevron is tapped.
 * @param onNext  Called when the right chevron is tapped.
 */
@Composable
fun MonthSelector(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(radii.rFull)
            .border(BorderStroke(1.dp, colors.border), radii.rFull)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        // Prev arrow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(radii.rFull)
                .clickable(onClick = onPrev),
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "Mes anterior",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )

        // Next arrow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(radii.rFull)
                .clickable(onClick = onNext),
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Mes siguiente",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
