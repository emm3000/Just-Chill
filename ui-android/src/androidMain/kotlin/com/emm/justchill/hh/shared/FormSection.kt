package com.emm.justchill.hh.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow

@Composable
fun FormSection(eyebrow: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val spacing = LocalEmmSpacing.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.s3)) {
        Eyebrow(text = eyebrow)
        content()
    }
}

@Preview
@Composable
private fun FormSectionPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            FormSection(eyebrow = "PERSONA") {
                Text(
                    text = "Juan",
                    style = LocalEmmType.current.bodyL,
                    color = LocalEmmColors.current.textPrimary,
                )
                Text(
                    text = "Contacto frecuente",
                    style = LocalEmmType.current.bodyM,
                    color = LocalEmmColors.current.textSecondary,
                )
            }
        }
    }
}
