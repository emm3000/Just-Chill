package com.emm.justchill.hh.shared.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun EmmTextFieldChill(
    value: String,
    placeholder: String,
    label: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    TextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onChange,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp
            )
        },
        label = {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        },
        textStyle = TextStyle(
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.onBackground
        ),
        maxLines = 1,
    )
}

@Composable
fun EmmTextInput(
    label: String,
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(modifier) {

        Text(
            text = label,
            fontWeight = FontWeight.Normal,
            fontFamily = LatoFontFamily,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(5.dp))

        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            value = value,
            onValueChange = onChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            },
            textStyle = TextStyle(
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onBackground
            ),
            maxLines = 2,
        )
    }
}

@PreviewLightDark
@Composable
fun TextFieldWithLabelPreview() {
    EmmTheme {
        Surface {
            EmmTextInput(
                label = "Cantidad",
                placeholder = "Ingrese una cantidad",
                value = "random",
                onChange = {  },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EmmTextFieldChild() {
    EmmTheme {
        Surface {
            EmmTextFieldChill(
                value = "Cantidad",
                placeholder = "Ingresa una cantidad",
                label = "Cantidad",
                onChange = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@PreviewLightDark
@Composable
fun TextFieldWithLabel2Preview() {
    EmmTheme {
        Surface {
            EmmTextInput(
                label = "",
                placeholder = "Ingrese una cantidad",
                value = "",
                onChange = {  },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}