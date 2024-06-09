package com.emm.retrofit.experiences.drinks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.emm.retrofit.core.Result
import com.emm.retrofit.core.theme.EmmTheme
import com.emm.retrofit.experiences.drinks.data.DrinkApiModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun Drinks(
    navController: NavController,
    mainViewModel: MainViewModel = koinViewModel(),
) {

    val state: Result<List<DrinkApiModel>> by mainViewModel.fetchDrinks.collectAsState()

    Drinks(state)
}

@Composable
private fun Drinks(
    state: Result<List<DrinkApiModel>>
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        if (state is Result.Success) {
            LazyColumn {
                items(state.data) { drinkItem ->
                    DrinkItem(drinkApiModel = drinkItem)
                }
            }
        }

        if (state is Result.Loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        if (state is Result.Failure) {
            Text(
                text = state.exception.message.orEmpty(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onError
            )
        }
    }
}

@Composable
fun DrinkItem(
    modifier: Modifier = Modifier,
    drinkApiModel: DrinkApiModel,
    navigateToDetail: () -> Unit = {},
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {

            }
            .padding(10.dp)
            .height(120.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(drinkApiModel.image)
                .crossfade(true)
                .build(),
            contentDescription = drinkApiModel.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .weight(1f)
                .clip(RoundedCornerShape(10))
        )

        Column(
            modifier = Modifier
                .weight(2f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = drinkApiModel.name.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = drinkApiModel.description,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrinkItemPreview() {
    EmmTheme {
        val drinkApiModel = DrinkApiModel(
            name = "lorem lorem lorem lorem lorem",
            description = "lorem lorem lorem lorem"
        )
        DrinkItem(drinkApiModel = drinkApiModel)
    }
}

@Preview(showBackground = true)
@Composable
fun DrinksPreview() {
    EmmTheme {
        Drinks(Result.Success(emptyList()))
    }
}