package com.emm.justchill.hh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.transaction.EmmCenteredToolbar
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(homeViewModel: HomeViewModel = koinViewModel()) {

    val homeState: HomeState by homeViewModel.calculators.collectAsStateWithLifecycle()

    Home(homeState = homeState)
}

@Composable
fun Home(homeState: HomeState) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        EmmCenteredToolbar(
            title = "Just Chill",
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        EmmHeading(text = homeState.account?.name.orEmpty())

        Spacer(modifier = Modifier.height(40.dp))

        EmmHeading(text = "Ingreso")

        EmmHeadlineMedium(text = "S/ ${homeState.income}")

        Spacer(modifier = Modifier.height(20.dp))

        EmmHeading(
            text = "Gasto",
            textColor = DeleteButtonColor,
        )
        EmmHeadlineMedium(
            text = "S/ ${homeState.spend}",
            textColor = DeleteButtonColor,
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@PreviewLightDark
@Composable
fun HomePreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Home(
            homeState = HomeState(
                income = "300.00",
                spend = "404.00"
            ),
        )
    }
}