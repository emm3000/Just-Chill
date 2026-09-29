package com.emm.justchill.feature.transaction.capture.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.atoms.FilledCta
import com.emm.justchill.core.ui.atoms.OutlinedCta
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

private const val NOTE_MAX_CHARS = 120

private const val FOCUS_DELAY_AFTER_SHEET_SLIDE_IN_MS = 150L

// No EmmSpacing step sits near 140dp.
private val NoteFieldMaxHeight: Dp = 140.dp

@Composable
fun NoteSheet(initialNote: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draft by remember {
        val initial = initialNote.take(NOTE_MAX_CHARS)
        mutableStateOf(TextFieldValue(text = initial, selection = TextRange(initial.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(FOCUS_DELAY_AFTER_SHEET_SLIDE_IN_MS)
        focusRequester.requestFocus()
    }

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
                .padding(horizontal = spacing.s5)
                .padding(bottom = spacing.s4)
                .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Nota",
                    style = type.titleM,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Opcional",
                    style = type.labelM,
                    color = colors.textTertiary,
                )
            }

            Spacer(Modifier.height(spacing.s4))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(radii.rM)
                    .background(colors.surface1)
                    .border(spacing.hairline, colors.border, radii.rM)
                    .padding(horizontal = spacing.s4, vertical = spacing.s3),
            ) {
                if (draft.text.isEmpty()) {
                    Text(
                        text = "Mercado Vea — pollo y verduras",
                        style = type.bodyM,
                        color = colors.textTertiary,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { newValue ->
                        if (newValue.text.length <= NOTE_MAX_CHARS) draft = newValue
                    },
                    textStyle = LocalTextStyle.current.merge(type.bodyM).copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.borderFocus),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = spacing.s16, max = NoteFieldMaxHeight)
                        .focusRequester(focusRequester),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Se guarda al confirmar el movimiento",
                    style = type.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${draft.text.length} / $NOTE_MAX_CHARS",
                    style = type.caption,
                    color = if (draft.text.length >= NOTE_MAX_CHARS) colors.danger else colors.textTertiary,
                )
            }

            Spacer(Modifier.height(spacing.s5))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                OutlinedCta(
                    label = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                FilledCta(
                    label = "Guardar nota",
                    onClick = {
                        onSave(draft.text)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
