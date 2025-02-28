package com.emm.justchill.me.loan.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.log
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.me.loan.domain.FrequencyType

@Composable
fun LoansRadioButton(
    modifier: Modifier = Modifier,
    selectedOption: FrequencyType = FrequencyType.DAILY,
    onOptionSelected: (FrequencyType) -> Unit = {},
) {

    val frequencyTypes: List<FrequencyType> = remember {
        FrequencyType.entries
    }

    Column(
        modifier = modifier,
    ) {

        frequencyTypes.forEach { transactionType ->
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .selectable(
                        selected = transactionType == selectedOption,
                        onClick = { onOptionSelected(transactionType) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = transactionType == selectedOption,
                    onClick = { onOptionSelected(transactionType) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = PrimaryButtonColor
                    )
                )
                Text(
                    text = transactionType.value,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = TextColor,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoansRadioButtonPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LoansRadioButton(
                modifier = Modifier
                    .log()
            )
            Text(text = "gaa")
        }
    }
}