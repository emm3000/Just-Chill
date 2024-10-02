package com.emm.justchill.daily

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.daily.domain.Driver
import org.koin.androidx.compose.koinViewModel

@Composable
fun DriversScreen(
    vm: DriversViewModel = koinViewModel(),
    navigateToSeeDailies: (Long) -> Unit,
    navigateToAddLoans: (Long) -> Unit,
    navigateToSeePayments: (String, String) -> Unit,
) {

    val a: Map<Driver, DriversScreenUi> by vm.drivers.collectAsState()
    val context: Context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val inputStream = context.contentResolver.openInputStream(uri)
        val json: String = inputStream?.bufferedReader().use { it?.readText() }.orEmpty()
        vm.import(json)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = {
                    vm.export()
                }) {
                    Text(
                        text = "EXPORTAR DATOS",
                        fontSize = 16.sp,
                        fontFamily = LatoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryButtonColor
                    )
                }
                TextButton(onClick = {
                    launcher.launch(arrayOf("application/json"))
                }) {
                    Text(

                        text = "IMPORTAR DATOS",
                        fontSize = 16.sp,
                        fontFamily = LatoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryButtonColor
                    )
                }
            }
        }
        items(a.keys.toList(), key = Driver::driverId) {
            DriverItem(
                driver = it,
                loans = a[it]?.loans.orEmpty(),
                navigateToSeeDailies = navigateToSeeDailies,
                navigateToAddLoans = navigateToAddLoans,
                addDaily = vm::addDaily,
                navigateToSeePayments = navigateToSeePayments,
                deleteLoan = vm::deleteLoan,
                dailies = a[it]?.dailies.orEmpty(),
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun DailyScreenPreview(modifier: Modifier = Modifier) {
    EmmTheme {
    }
}