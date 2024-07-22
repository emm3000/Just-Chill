package com.emm.justchill.hh.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun TextFieldWithLabel(
    modifier: Modifier = Modifier,
    label: String,
    value: String = "",
    onChange: (String) -> Unit = {},
) {

    Column(modifier.fillMaxWidth()) {
        Text(
            text = label, fontWeight = FontWeight.ExtraBold,
            fontFamily = LatoFontFamily
        )
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            value = value,
            onValueChange = onChange,
            placeholder = {
                Text(
                    text = label, fontWeight = FontWeight.Normal,
                    fontFamily = LatoFontFamily, color = Color.LightGray
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TextFieldWithLabelPreview() {
    EmmTheme {
        TextFieldWithLabel(
            label = "Cantidad"
        )
    }
}