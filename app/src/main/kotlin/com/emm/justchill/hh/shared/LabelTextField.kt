package com.emm.justchill.hh.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily

/**
 * Legacy label used inside [com.emm.justchill.hh.shared.EmmDropDown] and the legacy
 * amount input. New screens should use `EmmTextInput(label = ...)` from the design system.
 */
@Composable
fun LabelTextField(text: String) {
    Text(
        text = text,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 16.sp,
    )
}
