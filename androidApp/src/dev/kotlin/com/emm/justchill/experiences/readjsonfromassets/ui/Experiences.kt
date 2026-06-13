package com.emm.justchill.experiences.readjsonfromassets.ui

import com.emm.justchill.experiences.ExperienceItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emm.justchill.core.Result
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.experiences.toBundle
import org.koin.androidx.compose.koinViewModel

@Composable
fun Experiences(
    navController: NavController,
    vm: ExperiencesViewModel = koinViewModel(),
) {

    val state: Result<List<ExperienceItemUiState>> by vm.experiences.collectAsState()

    Experiences(state = state,)
}

@Composable
private fun Experiences(
    state: Result<List<ExperienceItemUiState>>,
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (state) {
            is Result.Success -> {
                items(state.data) { experience ->
                    ExperienceItem(experience.toBundle())
                }
            }

            is Result.Failure -> {
                item {
                    Text(text = state.exception.message.orEmpty())
                }
            }

            Result.Loading -> {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                    )
                }
            }
        }

    }
}


@Preview(showBackground = true)
@Composable
fun ExperiencesPreview() {
    EmmTheme {
        val experience = ExperienceItemUiState(
            title = "Simple list and detail from drinks api",
            description = "In this part, it was used jetpack compose components, (LazyColumn), In this part, it was used jetpack compose components, (LazyColumn)",
            date = "09/06/2024",
            category = "video",
            resource = "www.random.com",
            categoryColor = Color.Green

        )
        ExperienceItem(experience.toBundle())
    }
}