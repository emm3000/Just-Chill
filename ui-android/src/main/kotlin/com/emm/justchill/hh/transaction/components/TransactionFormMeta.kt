package com.emm.justchill.hh.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Date and note are inline links, not rows: neither is a decision the user has to make — the day
 * defaults to today and a note is optional — so neither may claim a row's worth of the form.
 */
@Composable
internal fun FormMetaRow(
    dateLabel: String,
    note: String,
    onDateClick: () -> Unit,
    onNoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = modifier.height(spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s5),
    ) {
        DateAction(label = dateLabel, onClick = onDateClick)
        MetaDivider()
        NoteAction(note = note, onClick = onNoteClick, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun DateAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
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
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    val empty = note.isBlank()

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
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
            .width(1.dp)
            .height(spacing.s3)
            .background(colors.border),
    )
}
