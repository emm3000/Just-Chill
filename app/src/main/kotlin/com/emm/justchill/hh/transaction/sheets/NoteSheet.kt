package com.emm.justchill.hh.transaction.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.PlexMonoFontFamily
import com.emm.justchill.core.ui.atoms.SheetDragHandle

private const val NOTE_MAX_CHARS = 120

/**
 * Bottom sheet for editing the transaction note.
 *
 * Local draft state is initialized from [initialNote]; nothing is saved until the
 * user taps "Guardar nota", which calls [onSave] with the final text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSheet(
    initialNote: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draft by remember { mutableStateOf(initialNote.take(NOTE_MAX_CHARS)) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .imePadding(),
        ) {
            // ── Header: "Nota" + "Opcional" ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Nota",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.08).sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Opcional",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Multi-line note field ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(radii.rM)
                    .background(colors.surface1)
                    .border(1.dp, colors.border, radii.rM)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (draft.isEmpty()) {
                    Text(
                        text = "Mercado Vea — pollo y verduras",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                        fontFamily = InterFontFamily,
                        color = colors.textTertiary,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { newValue ->
                        if (newValue.length <= NOTE_MAX_CHARS) draft = newValue
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                        fontFamily = InterFontFamily,
                        lineHeight = 20.sp,
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp, max = 140.dp)
                        .focusRequester(focusRequester),
                )
            }

            // ── Helper row: hint + char counter ────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Se guarda al confirmar el movimiento",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W400,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${draft.length} / $NOTE_MAX_CHARS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = PlexMonoFontFamily,
                    color = if (draft.length >= NOTE_MAX_CHARS) colors.danger else colors.textTertiary,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Cancel + Save buttons ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetButton(
                    label = "Cancelar",
                    primary = false,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                SheetButton(
                    label = "Guardar nota",
                    primary = true,
                    onClick = {
                        onSave(draft)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SheetButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    val bg = if (primary) colors.textPrimary else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (primary) colors.bg else colors.textPrimary
    val borderColor = if (primary) colors.textPrimary else colors.border

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(radii.rM)
            .background(bg)
            .border(1.dp, borderColor, radii.rM)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = fg,
            textAlign = TextAlign.Center,
        )
    }
}
