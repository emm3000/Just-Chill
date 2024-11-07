package com.emm.justchill.hh.shared.shared

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun EmmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {

    FilledTonalButton(
        modifier = modifier,
        onClick = dropUnlessResumed { onClick() },
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(25)
    ) {
        Text(
            text = text,
            fontFamily = LatoFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@PreviewLightDark
@Composable
private fun EmmPrimaryButtonPreview() {
    EmmTheme {
        EmmPrimaryButton(
            text = "Save",
            onClick = {},
            enabled = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@PreviewLightDark
@Composable
private fun EmmPrimaryButton2Preview() {
    EmmTheme {
        EmmPrimaryButton(
            text = "Save",
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}