package com.emm.justchill.loans.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.daily.domain.Driver

@Composable
fun HomeScreen(
    drivers: List<Driver>,
    navigateToDriverView: (Long) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 30.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        LazyColumn {
            items(drivers, key = Driver::driverId) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navigateToDriverView(it.driverId)
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = it.name,
                        modifier = Modifier,
                        fontSize = 20.sp,
                        fontFamily = LatoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                    IconButton(onClick = {
                        navigateToDriverView(it.driverId)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextColor
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoansHomePreview(modifier: Modifier = Modifier) {
    EmmTheme {
        HomeScreen(
            drivers = listOf(
                Driver(driverId = 0, name = "Juanutio"),
                Driver(driverId = 1, name = "Juanutio 2")
            ),
            navigateToDriverView = {}
        )
    }
}