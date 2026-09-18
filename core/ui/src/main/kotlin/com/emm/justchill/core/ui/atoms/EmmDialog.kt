package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun EmmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmTone: IconBtnTone = IconBtnTone.Accent,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(radii.rXL)
                .background(colors.surface1)
                .border(width = 1.dp, color = colors.border, shape = radii.rXL)
                .padding(spacing.s5),
        ) {
            Text(text = title, style = type.titleM, color = colors.textPrimary)

            if (content != null) {
                Column(modifier = Modifier.padding(top = spacing.s3)) {
                    content()
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s2, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s4),
            ) {
                DialogAction(label = dismissLabel, onClick = onDismiss, tone = IconBtnTone.Neutral)
                DialogAction(label = confirmLabel, onClick = onConfirm, tone = confirmTone)
            }
        }
    }
}

@Composable
private fun DialogAction(
    label: String,
    onClick: () -> Unit,
    tone: IconBtnTone,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current

    val textColor = when (tone) {
        IconBtnTone.Neutral -> colors.textSecondary
        IconBtnTone.Accent -> colors.accent
        IconBtnTone.Danger -> colors.danger
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(radii.rS)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        Text(text = label, style = type.labelL, color = textColor)
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun EmmDialogPreview() {
    EmmTheme {
        EmmDialog(
            title = "¿Borrar «Supermercado»?",
            confirmLabel = "Borrar",
            onConfirm = {},
            dismissLabel = "Cancelar",
            onDismiss = {},
            onDismissRequest = {},
            confirmTone = IconBtnTone.Danger,
            content = {
                Text(
                    text = "1 movimiento va a quedar sin categoría. No puedes deshacerlo desde la app.",
                    style = LocalEmmType.current.bodyM,
                    color = LocalEmmColors.current.textSecondary,
                )
            },
        )
    }
}
