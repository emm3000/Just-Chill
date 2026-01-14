package com.emm.justchill.hh.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.log
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily

@Composable
fun NewAccountItem(
    modifier: Modifier = Modifier,
    accountName: String,
    balance: Double,
    accountType: String,
    onClick: () -> Unit = {},
) {

    Row(
        modifier = modifier
            .padding(vertical = 6.dp)
            .clickable {
                onClick()
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.primaryFixed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = accountName,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            Text(
                text = accountType,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontFamily = LatoFontFamily,
                fontSize = 15.sp,
            )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = "S/ $balance",
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            Text(
                text = "Current Balance",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontFamily = LatoFontFamily,
                fontSize = 15.sp,
            )
        }
    }
}

@Preview
@Composable
private fun NewAccountItemPreview() {
    EmmTheme {
        Surface(
            modifier = Modifier.log().padding(20.dp)
        ) {
            NewAccountItem(
                modifier = Modifier.fillMaxWidth(),
                accountName = "Main Bank Account",
                balance = 10_000.0,
                accountType = "Savings"
            )
        }
    }
}