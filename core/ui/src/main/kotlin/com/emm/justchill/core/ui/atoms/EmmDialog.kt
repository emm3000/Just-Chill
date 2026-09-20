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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Suppress("LongParameterList")
@Composable
fun EmmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = onDismiss,
    properties: DialogProperties = DialogProperties(),
    actionsEnabled: Boolean = true,
    confirmTone: IconBtnTone = IconBtnTone.Primary,
    destructiveAction: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors: EmmColors = LocalEmmColors.current
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
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

            if (destructiveAction != null) {
                Box(modifier = Modifier.padding(top = spacing.s3)) {
                    destructiveAction()
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s2, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s4),
            ) {
                DialogAction(
                    label = dismissLabel,
                    onClick = onDismiss,
                    tone = IconBtnTone.Neutral,
                    enabled = actionsEnabled,
                )
                DialogAction(
                    label = confirmLabel,
                    onClick = onConfirm,
                    tone = confirmTone,
                    enabled = actionsEnabled,
                )
            }
        }
    }
}

@Composable
fun DialogAction(
    label: String,
    onClick: () -> Unit,
    tone: IconBtnTone,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors: EmmColors = LocalEmmColors.current
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    val toneColor: Color = when (tone) {
        IconBtnTone.Neutral -> colors.textSecondary
        IconBtnTone.Primary -> colors.textPrimary
        IconBtnTone.Danger -> colors.danger
    }
    val textColor: Color = if (enabled) toneColor else colors.textDisabled

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(radii.rS)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.s2),
    ) {
        Text(text = label, style = type.labelL, color = textColor)
    }
}

/**
 * A write already in flight lands whatever happens here, so every gesture the user reads as
 * "abort" — the scrim, the back press, the dismiss button — has to stop until it does.
 */
fun inFlightDialogProperties(isInFlight: Boolean): DialogProperties = DialogProperties(
    dismissOnBackPress = !isInFlight,
    dismissOnClickOutside = !isInFlight,
)

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

@Preview
@Composable
private fun EmmDialogBlockedPreview() {
    EmmTheme {
        EmmDialog(
            title = "¿Borrar «Supermercado»?",
            confirmLabel = "Borrar",
            onConfirm = {},
            dismissLabel = "Cancelar",
            onDismiss = {},
            onDismissRequest = {},
            properties = inFlightDialogProperties(isInFlight = true),
            actionsEnabled = false,
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
