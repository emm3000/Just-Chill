package com.emm.justchill

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ExperienceItem(
    data: ExperienceBundle,
    toDetail: (ExperienceBundle) -> Unit = {},
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
                    toDetail(data)
                }
                .padding(10.dp)
        ) {

            Text(
                text = data.title,
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
                    text = data.description,
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
                    text = data.date,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = data.category.uppercase(),
                    modifier = Modifier
                        .border(2.dp, Color.DarkGray, RoundedCornerShape(15))
                        .padding(vertical = 5.dp, horizontal = 10.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ClickableText(
                text = AnnotatedString(data.resource),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Blue.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
            ) { }
        }
    }
}