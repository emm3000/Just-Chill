package com.emm.justchill.experiences.drinks.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.emm.justchill.core.theme.EmmTheme

@Composable
fun DrinkDetailScreen(drink: DrinkDetail) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(drink.image)
                .crossfade(true)
                .build(),
            contentDescription = drink.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(10))
        )

        Text(
            text = drink.title,
            style = MaterialTheme.typography.displayLarge,
        )

        Text(
            text = drink.description,
            style = MaterialTheme.typography.headlineLarge,
        )
    }

}

@Preview(showBackground = true)
@Composable
fun DrinkDetailPreview(modifier: Modifier = Modifier) {
    val fakeDrink = DrinkDetail(
        title = "Margarita",
        description = "lorem",
        image = "lorem"
    )
    EmmTheme {
        DrinkDetailScreen(drink = fakeDrink)
    }
}