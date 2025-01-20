package com.emm.justchill.newhh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.components.HhCard
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhOnBackgroundColor
import com.emm.justchill.core.theme.HhSecondaryTextBackground
import com.emm.justchill.hh.transaction.presentation.TransactionType
import com.emm.justchill.hh.transaction.presentation.TransactionUi

@Composable
fun Spend(
    transactions: List<TransactionUi>,
    modifier: Modifier = Modifier,
) {

    LazyColumn(
        modifier = modifier
            .background(HhBackgroundColor)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(transactions) {
            SpendItem(it)
        }
    }
}

@Composable
fun SpendItem(transactionUi: TransactionUi) {

    HhCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = transactionUi.description,
                    color = HhOnBackgroundColor,
                    fontSize = 18.sp,
                    maxLines = 1,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${transactionUi.readableDate}, ${transactionUi.readableTime}",
                    color = HhSecondaryTextBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = TextUnit(1f, TextUnitType.Em),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = transactionUi.description,
                tint = HhOnBackgroundColor,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpendPreview() {
    EmmTheme {
        val items = remember {
            (1..100).map {
                TransactionUi(
                    transactionId = it.toString(),
                    type = TransactionType.INCOME,
                    amount = "amount",
                    description = "description",
                    date = 2L,
                    readableDate = "readableDate",
                    readableTime = "readableTime"
                )
            }
        }
        Spend(
            transactions = items,
            modifier = Modifier.fillMaxSize()
        )
    }
}