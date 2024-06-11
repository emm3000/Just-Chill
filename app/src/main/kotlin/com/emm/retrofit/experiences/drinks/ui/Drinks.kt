package com.emm.retrofit.experiences.drinks.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Drinks(
        state = state,
        value = mainViewModel.searchText,
        updateValue = mainViewModel::updateSearchText,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Drinks(
    state: Result<List<DrinkApiModel>>,
    value: String,
    updateValue: (String) -> Unit = {}
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {

        stickyHeader {
            Surface {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = value,
                    onValueChange = updateValue,
                    placeholder = {
                        Text(
                            text = "buscar",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = 0.5f)
                        )
                    },
                )
            }
        }

        if (state is Result.Success) {
            items(state.data) {
                DrinkItem(drinkApiModel = it)
            }
        }

        if (state is Result.Loading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier
                )
            }
        }

        if (state is Result.Failure) {
            item {
                Text(
                    text = state.exception.message.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
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
        Drinks(Result.Failure(Exception("gaaa")), "")
    }
}