package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun FormMetaRow(
    dateLabel: String,
    note: String,
    onDateClick: () -> Unit,
    onNoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Row(
        modifier = modifier.height(spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s5),
    ) {
        DateAction(label = dateLabel, onClick = onDateClick)
        MetaDivider()
        NoteAction(
            note = note,
            onClick = onNoteClick,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(min = spacing.s12),
        )
    }
}

@Composable
private fun DateAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    // "Hoy" and its chevron measure under 48dp both ways; the row is stretched to the floor
    // rather than centred at its intrinsic size.
    Row(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = spacing.s12)
            .clip(radii.rXS)
            .clickable(
                onClickLabel = "Cambiar la fecha",
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        Text(text = label, style = type.labelL, color = colors.textPrimary)
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(spacing.s3),
        )
    }
}

@Composable
private fun NoteAction(note: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    val empty = note.isBlank()

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(radii.rXS)
            .clickable(
                onClickLabel = if (empty) "Agregar una nota" else "Editar la nota",
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        if (empty) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(spacing.s3),
            )
        }
        Text(
            text = if (empty) "Agregar nota" else note,
            style = type.labelL,
            fontStyle = if (empty) FontStyle.Normal else FontStyle.Italic,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetaDivider() {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Box(
        modifier = Modifier
            .width(spacing.hairline)
            .height(spacing.s3)
            .background(colors.border),
    )
}
