package com.emm.justchill.hh.me.daily.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DailyScreen(
    driverId: Long,
    vm: DailiesViewModel = koinViewModel(
        parameters = { parametersOf(driverId) }
    ),
) {

    val dailies by vm.dailies.collectAsState()
    val driver by vm.driver.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            Text(
                modifier = Modifier,
                text = driver?.name.orEmpty(),
                color = TextColor,
                fontFamily = LatoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }

        items(dailies) {
            DailyItem(it, vm::deleteDaily)
        }
    }
}