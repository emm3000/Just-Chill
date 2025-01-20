package com.emm.justchill.hh.transaction.presentation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhOnBackgroundColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmmToolbarTitle(
    title: String,
    navigationIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = HhBackgroundColor,
        ),
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = HhOnBackgroundColor,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = dropUnlessResumed {
                    navigationIconClick()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = null,
                    tint = HhOnBackgroundColor,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmmCenteredToolbar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {

    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = HhBackgroundColor,
        ),
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = HhOnBackgroundColor,
            )
        },
        actions = actions
    )
}

@Preview(showBackground = true)
@Composable
private fun EmmCenteredToolbarPreview() {
    EmmTheme {
        EmmCenteredToolbar(
            title = "Transacciones",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmmToolbarTitlePreview() {
    EmmTheme {
        EmmToolbarTitle(
            title = "Add transaction",
            navigationIconClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}