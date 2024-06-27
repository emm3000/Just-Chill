package com.emm.justchill.experiences.hh.presentation.home

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emm.justchill.Transactions
import com.emm.justchill.core.Result
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.experiences.hh.presentation.Category
import com.emm.justchill.experiences.hh.presentation.Income
import com.emm.justchill.experiences.hh.presentation.SeeTransactions
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(
    navController: NavController,
    homeViewModel: HomeViewModel = koinViewModel(),
) {

    val state by homeViewModel.transactions.collectAsState()

    Home(
        state = state,
        navigateToIncome = { navController.navigate(Income) },
        navigateToCategory = { navController.navigate(Category) },
        navigateToSeeTransactions = { navController.navigate(SeeTransactions) }
    )
}

@Composable
fun Home(
    state: Result<List<Transactions>>,
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

        if (state is Result.Success) {
            state.data.forEach {
                Text(text = it.description)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview(modifier: Modifier = Modifier) {
    EmmTheme {
    }
}