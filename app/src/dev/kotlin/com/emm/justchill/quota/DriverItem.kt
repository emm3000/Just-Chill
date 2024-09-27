package com.emm.justchill.quota

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.quota.domain.Driver

@Composable
fun DriverItem(
    driver: Driver,
    navigateToSeeQuotas: (Long) -> Unit,
    navigateToAddQuotas: (Long) -> Unit,
    navigateToSeeLoans: (Long) -> Unit,
    navigateToAddLoans: (Long) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            text = driver.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextColor,
            fontFamily = LatoFontFamily
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {
                    navigateToSeeQuotas(driver.driverId)
                },
                label = {
                    Text("VER FERIAS", color = TextColor)
                }
            )
            AssistChip(
                onClick = {
                    navigateToAddQuotas(driver.driverId)
                },
                label = {
                    Text("AGREGAR PAGO", color = TextColor)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {
                    navigateToSeeLoans(driver.driverId)
                },
                label = {
                    Text("VER PRESTAMOS", color = TextColor)
                }
            )
            AssistChip(
                onClick = {
                    navigateToAddLoans(driver.driverId)
                },
                label = {
                    Text("AGREGAR PRESTAMOS", color = TextColor)
                }
            )
        }


        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }
}

@Preview
@Composable
fun ItemPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        DriverItem(
            driver = Driver(driverId = 0, name = "Random name"),
            navigateToSeeQuotas = {},
            navigateToAddQuotas = {},
            navigateToAddLoans = {}, navigateToSeeLoans = {},
        )
    }
}