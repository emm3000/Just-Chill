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
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.Eyebrow

@Composable
fun FormSection(eyebrow: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    fontSize = 18.sp,
                    fontFamily = InterFontFamily,
                    color = LocalEmmColors.current.textPrimary,
                )
                Text(
                    text = "Contacto frecuente",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    color = LocalEmmColors.current.textSecondary,
                )
            }
        }
    }
}
