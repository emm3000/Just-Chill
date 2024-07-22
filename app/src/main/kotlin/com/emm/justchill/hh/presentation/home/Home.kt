package com.emm.justchill.hh.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(
    homeViewModel: HomeViewModel = koinViewModel(),
) {

    val sumTransactions by homeViewModel.sumTransactions.collectAsState()
    val difference by homeViewModel.difference.collectAsState()

    Home(
        sumTransaction = sumTransactions,
        difference = difference,
    )
}

@Composable
fun Home(
    sumTransaction: Pair<String, String> = Pair("", ""),
    difference: String = "",
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        Text(text = "Hh", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Ingreso", fontWeight = FontWeight.Bold)
        Text(
            text = "S/ ${sumTransaction.first}",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(text = "Gasto", fontWeight = FontWeight.Bold, color = Color.Red)
        Text(
            text = "S/ ${sumTransaction.second}",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "S/ $difference",
            fontSize = 25.sp,
            color = if (difference.contains("-")) {
                Color.Red
            } else {
                Color.Unspecified
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Home(
            sumTransaction = Pair("200.50", "502.5"),
            difference = "200.00"
        )
    }
}