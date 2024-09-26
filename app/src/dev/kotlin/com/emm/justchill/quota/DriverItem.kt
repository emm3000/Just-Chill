package com.emm.justchill.quota

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.quota.domain.Driver

@Composable
fun DriverItem(
    driver: Driver,
    navigateToQuotas: (Long) -> Unit,
    navigateToAddQuota: (Long) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .clickable {
                navigateToQuotas(driver.driverId)
            }
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = driver.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextColor,
                fontFamily = LatoFontFamily
            )
            IconButton(
                onClick = {
                    navigateToAddQuota(driver.driverId)
                }
            ) {
                Icon(imageVector = Icons.Default.Add, null, tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }
}