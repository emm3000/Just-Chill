package com.emm.retrofit.experiences.readassets.ui

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
import com.emm.retrofit.core.Result
import com.emm.retrofit.core.theme.EmmTheme
import com.emm.retrofit.experiences.readassets.domain.Experience
import org.koin.androidx.compose.koinViewModel

@Composable
fun Experiences(
    vm: ExperiencesViewModel = koinViewModel(),
) {

    val state: Result<List<Experience>> by vm.experiences.collectAsState()

    Experiences(state)
}

@Composable
private fun Experiences(
    state: Result<List<Experience>>,
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (state) {
            is Result.Success -> {
                items(state.data) {
                    ExperienceItem(it)
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
fun ExperienceItem(experience: Experience) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {

            Text(
                text = experience.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Column(
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = experience.date,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = experience.category,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            ClickableText(
                text = AnnotatedString(experience.resource),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Blue.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
            ) {

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExperiencesPreview() {
    EmmTheme {
        val experience = Experience(
            title = "Simple list and detail from drinks api",
            description = "In this part, it was used jetpack compose components, (LazyColumn), In this part, it was used jetpack compose components, (LazyColumn)",
            date = "09/06/2024",
            category = "video",
            resource = "www.random.com"

        )
        ExperienceItem(
            experience = experience
        )
    }
}