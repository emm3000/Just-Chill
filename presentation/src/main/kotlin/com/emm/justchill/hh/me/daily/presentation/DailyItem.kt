package com.emm.justchill.hh.me.daily.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyItem(
    daily: DailyUi,
    deleteDaily: (String) -> Unit,
) {

    var showOptions: Boolean by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    showOptions = true
                }
            )
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier,
                ) {
                    Text(
                        text = daily.day,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor,
                        fontFamily = LatoFontFamily
                    )
                    Text(
                        text = daily.dayNumber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor,
                        fontFamily = LatoFontFamily
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier,
                    text = daily.amount,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFontFamily,
                    color = TextColor,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    modifier = Modifier,
                    text = daily.readableTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = TextUnit(1f, TextUnitType.Em),
                    color = TextColor.copy(alpha = .8f),
                    fontFamily = LatoFontFamily
                )

            }

        }

        AnimatedVisibility(showOptions) {
            Row {
                TextButton(onClick = {
                    deleteDaily(daily.dailyId)
                }) {
                    Text(
                        text = "Eliminar feria",
                        style = TextStyle(
                            fontFamily = LatoFontFamily,
                            color = DeleteButtonColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                TextButton(onClick = {
                    showOptions = false
                }) {
                    Text(
                        text = "Cancelar",
                        style = TextStyle(
                            fontFamily = LatoFontFamily,
                            color = TextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
    }

}

@Preview(showBackground = true)
@Composable
fun Preview() {
    EmmTheme {
        DailyItem(
            daily = DailyUi(
                dailyId = "",
                amount = "S./ 22",
                dailyDate = 0,
                driverId = 0,
                readableTime = "random",
                day = "LUN",
                dayNumber = "20"
            ),
            deleteDaily = {}
        )
    }
}