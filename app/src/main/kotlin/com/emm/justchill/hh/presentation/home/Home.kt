package com.emm.justchill.hh.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
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
import androidx.navigation.NavController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.presentation.Category
import com.emm.justchill.hh.presentation.Income
import com.emm.justchill.hh.presentation.SeeTransactions
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(
    navController: NavController,
    homeViewModel: HomeViewModel = koinViewModel(),
) {

    val sumTransactions by homeViewModel.sumTransactions.collectAsState()
    val difference by homeViewModel.difference.collectAsState()

    Home(
        sumTransaction = sumTransactions,
        difference = difference,
        navigateToIncome = { navController.navigate(Income) },
        navigateToCategory = { navController.navigate(Category) },
        navigateToSeeTransactions = { navController.navigate(SeeTransactions) }
    )
}

@Composable
fun Home(
    sumTransaction: Pair<String, String> = Pair("", ""),
    difference: String = "",
    navigateToIncome: () -> Unit = {},
    navigateToCategory: () -> Unit = {},
    navigateToSeeTransactions: () -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Hh")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Ingreso", fontWeight = FontWeight.Bold)
                Text(
                    text = "S/ ${sumTransaction.first}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Gasto", fontWeight = FontWeight.Bold, color = Color.Red)
                Text(
                    text = "S/ ${sumTransaction.second}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                )
            }

        }
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

        Spacer(modifier = Modifier.height(30.dp))

        FilledTonalButton(onClick = navigateToIncome, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ingresar Transacción")
        }

        FilledTonalButton(onClick = navigateToCategory, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Crear categorías")
        }

        FilledTonalButton(onClick = navigateToSeeTransactions, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ver transacciones")
        }

        FilledTonalButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ver Gráficas")
        }
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