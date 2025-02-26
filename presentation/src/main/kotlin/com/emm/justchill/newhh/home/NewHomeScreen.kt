package com.emm.justchill.newhh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhOnBackgroundColor
import com.emm.justchill.hh.transaction.presentation.TransactionType
import com.emm.justchill.hh.transaction.presentation.TransactionUi
import kotlinx.coroutines.CoroutineScope

@Composable
fun NewHomeScreen(
    transactions: List<TransactionUi>,
    modifier: Modifier = Modifier
) {

    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val state = rememberPagerState(initialPage = 0) { 2 }
    var selectedTabIndex by remember {
        mutableIntStateOf(0)
    }

    Column(
        modifier = modifier
            .background(HhBackgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(10.dp))
        Tabs(state)
        HorizontalPager(state = state) {
            Spend(
                transactions = transactions,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Tabs(state: PagerState) {
    TabRow(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .clip(RoundedCornerShape(30)),
        selectedTabIndex = state.currentPage,
        containerColor = HhOnBackgroundColor,
    ) {
        Tab(
            modifier = Modifier.height(40.dp),
            selected = state.currentPage == 0,
            onClick = {},
        ) {
            Text(
                text = "Gastos",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Tab(
            selected = state.currentPage == 1,
            onClick = {},
        ) {
            Text(
                text = "Ingresos",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview
@Composable
private fun NewHomeScreenPreview() {
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
        NewHomeScreen(
            transactions = items,
        )
    }
}