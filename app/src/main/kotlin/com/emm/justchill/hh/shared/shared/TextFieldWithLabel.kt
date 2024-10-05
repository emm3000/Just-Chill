package com.emm.justchill.hh.shared.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.auth.presentation.LabelTextField
import com.emm.justchill.hh.transaction.presentation.TransactionLabel

@Composable
fun TextFieldWithLabel(
    modifier: Modifier = Modifier,
    label: String,
    placeholder: String = label,
    value: String = "",
    onChange: (String) -> Unit = {},
) {

    Column(modifier.fillMaxWidth()) {
        TransactionLabel(text = label)
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            value = value,
            onValueChange = onChange,
            placeholder = {
                LabelTextField(placeholder)
            },
            textStyle = TextStyle(
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = TextColor,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextColor
            ),
            maxLines = 2,
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