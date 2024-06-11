package com.emm.justchill.experiences.readassets.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emm.justchill.core.Result
import com.emm.justchill.core.navigation.Drink
import com.emm.justchill.core.theme.EmmTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun Experiences(
    navController: NavController,
    vm: ExperiencesViewModel = koinViewModel(),
) {

    val state: Result<List<ExperienceItemUiState>> by vm.experiences.collectAsState()

    Experiences(
        state = state,
        navigateTo = {
            navController.navigate(route = Drink)
        },
    )
}

@Composable
private fun Experiences(
    state: Result<List<ExperienceItemUiState>>,
    navigateTo: () -> Unit,
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
                    ExperienceItem(
                        experience = experience,
                        navigateTo = navigateTo
                    )
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

@Composable
fun ExperienceItem(
    experience: ExperienceItemUiState,
    navigateTo: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navigateTo()
                }
                .padding(10.dp)
        ) {

            Text(
                text = experience.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = experience.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = experience.date,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = experience.category.uppercase(),
                    modifier = Modifier
                        .border(2.dp, experience.categoryColor, RoundedCornerShape(15))
                        .padding(vertical = 5.dp, horizontal = 10.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ClickableText(
                text = AnnotatedString(experience.resource),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Blue.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
            ) {  }
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
        ExperienceItem(
            experience = experience
        )
    }
}