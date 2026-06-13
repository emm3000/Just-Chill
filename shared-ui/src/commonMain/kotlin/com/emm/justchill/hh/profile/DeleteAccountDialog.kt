package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    val typography = LocalEmmType.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface2)
                .padding(20.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.negMuted)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "¿Eliminar tu cuenta?",
                style = typography.headlineM,
                color = colors.textPrimary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Se borra tu cuenta y todos tus datos en la nube. " +
                    "Tu plata sigue acá, en este teléfono — eso no se toca.",
                style = typography.bodyM,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DialogActionButton(
                    label = "Cancelar",
                    bg = colors.surface1,
                    border = colors.border,
                    textColor = colors.textPrimary,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                DialogActionButton(
                    label = "Eliminar cuenta",
                    bg = colors.danger,
                    border = colors.danger,
                    textColor = colors.textOnAccent,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DialogActionButton(
    label: String,
    bg: Color,
    border: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalEmmType.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
    ) {
        Text(
            text = label,
            style = type.titleM,
            color = textColor,
        )
    }
}

@Preview
@Composable
private fun DeleteAccountDialogPreview() {
    EmmTheme {
        DeleteAccountDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
