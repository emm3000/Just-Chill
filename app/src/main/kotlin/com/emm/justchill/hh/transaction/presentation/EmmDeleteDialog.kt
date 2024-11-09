package com.emm.justchill.hh.transaction.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun EmmDeleteDialog(
    setShowDeleteDialog: (Boolean) -> Unit,
    onConfirmButton: () -> Unit,
    modifier: Modifier = Modifier,
) {

    AlertDialog(
        modifier = modifier,
        onDismissRequest = { setShowDeleteDialog(false) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        text = {
            Text(
                text = "Estas seguro de eliminar esta transacción.",
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        },
        title = {
            Text(
                text = "Eliminar transacción",
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmButton()
            }) {
                Text(
                    text = "Confirmar",
                    fontSize = 16.sp,
                    color = DeleteButtonColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFontFamily
                )
            }

        },
        dismissButton = {
            TextButton(onClick = { setShowDeleteDialog(false) }) {
                Text(
                    text = "Cancelar",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFontFamily
                )
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun EmmDeleteDialogPreview() {
    EmmTheme {
        EmmDeleteDialog(
            setShowDeleteDialog = {},
            onConfirmButton = {},
            modifier = Modifier
        )
    }
}