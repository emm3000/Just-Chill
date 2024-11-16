package com.emm.justchill.hh.fasttransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.home.EmmHeadlineMedium
import com.emm.justchill.hh.shared.shared.EmmPrimaryButton

@Composable
fun FastTransaction(modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        EmmHeadlineMedium(
            text = "Agregar transacción",
            textColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier,
        )

        Spacer(Modifier.height(15.dp))

        EmmBoldText(
            text = "Fecha actual: 10 de nov. 3:05",
            modifier = Modifier.fillMaxWidth()
        )

        EmmBoldText(
            text = "Ingrese un monto",
            modifier = Modifier.fillMaxWidth(),
        )
        EmmAmountChill(
            value = TextFieldValue("123"),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth()
        )

        val (text, setText) = remember { mutableStateOf("") }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = setText,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            label = {
                Text(
                    text = "Descripción",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface
            ),
            placeholder = {
                Text(
                    text = "Descripción",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        )

        EmmPrimaryButton(
            text = "Guardar",
            onClick = {},
            enabled = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@PreviewLightDark
@Composable
private fun FastTransactionPreview() {
    EmmTheme {
        FastTransaction(
            modifier = Modifier.fillMaxSize()
        )
    }
}