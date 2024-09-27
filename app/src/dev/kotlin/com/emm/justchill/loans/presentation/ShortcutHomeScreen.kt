package com.emm.justchill.loans.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.PrimaryDisableButtonColor

@Composable
fun ShortcutHomeScreen(
    navigateToLoans: () -> Unit,
    navigateToPayments: () -> Unit,
    navigateToQuota: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 30.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = navigateToLoans,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryButtonColor,
                disabledContainerColor = PrimaryDisableButtonColor
            ),
            shape = RoundedCornerShape(20)
        ) {
            Text(
                text = "xxx",
                fontFamily = LatoFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(13.dp))
        Button(
            onClick = navigateToPayments,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryButtonColor,
                disabledContainerColor = PrimaryDisableButtonColor
            ),
            shape = RoundedCornerShape(20)
        ) {
            Text(
                text = "xxx",
                fontFamily = LatoFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(13.dp))
        Button(
            onClick = navigateToQuota,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryButtonColor,
                disabledContainerColor = PrimaryDisableButtonColor
            ),
            shape = RoundedCornerShape(20)
        ) {
            Text(
                text = "Ferias",
                fontFamily = LatoFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoansHomePreview(modifier: Modifier = Modifier) {
    EmmTheme {
        ShortcutHomeScreen({}, {}, {})
    }
}