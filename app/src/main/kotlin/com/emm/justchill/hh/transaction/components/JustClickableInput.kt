package com.emm.justchill.hh.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LatoFontFamily

/**
 * Legacy clickable read-only input. Kept temporarily for [EditTransaction];
 * delete when EditTransaction is migrated to the design system.
 */
@Composable
fun JustClickableInput(
    value: String,
    label: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFontFamily,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 17.sp,
        )
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.onBackground,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            ),
            placeholder = {
                Text(
                    text = "Selecciona",
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                )
            },
            textStyle = TextStyle(
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
            ),
        )
    }
}
