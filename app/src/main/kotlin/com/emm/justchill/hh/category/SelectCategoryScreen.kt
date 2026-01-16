package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.category.CategoryType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.transaction.SelectableCategory
import com.emm.justchill.hh.transaction.components.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.components.NewButton
import kotlinx.coroutines.launch

@Composable
fun SelectCategoryScreen(
    onCategorySelected: (SelectableCategory) -> Unit,
    onBack: () -> Unit,
    onNewCategory: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
    value: String,
    income: List<SelectableCategory>,
    expense: List<SelectableCategory>,
) {

    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            EmmCenteredToolbar(
                title = "Seleccionar Categoría",
                navigationIconClick = Icons.Default.ArrowBackIosNew,
                onNavigationIconClick = {
                    onBack()
                },
            )
        },
        bottomBar = {
            NewButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
                    .padding(bottom = 10.dp)
                    .navigationBarsPadding(),
                onClick = onNewCategory,
                title = "Crear nueva categoría",
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = value,
                onValueChange = {
                    onValueChange(it)
                },
                placeholder = {
                    Text(
                        text = "Search categories . . .",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            SecondaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier,
                containerColor = Color.Transparent,
                edgePadding = 1.dp,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                        height = 3.dp,
                        color = Color(0xFF00A3FF)
                    )
                }
            ) {
                Tab(
                    modifier = Modifier.height(50.dp),
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                ) {
                    Text(
                        text = "Income",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = LatoFontFamily
                    )
                }
                Tab(
                    modifier = Modifier.height(50.dp),
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                ) {
                    Text(
                        text = "Expense",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = LatoFontFamily
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                when (it) {
                    0 -> CategoryItemPicker(income) {
                        onCategorySelected(it)
                    }
                    1 -> CategoryItemPicker(expense) {
                        onCategorySelected(it)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItemPicker(
    categories: List<SelectableCategory>,
    onCategorySelected: (SelectableCategory) -> Unit = {},
) {

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {

        items(categories, key = SelectableCategory::categoryId) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onCategorySelected(it)
                    }
                    .padding(15.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(it.color.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it.icon.icon,
                        contentDescription = it.name,
                        tint = it.color.primary,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = it.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = LatoFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SelectCategoryScreenPreview() {
    EmmTheme {
        val categories = remember {
            buildList {
                repeat(7) {
                    add(
                        SelectableCategory(
                            categoryId = "$it nominavi",
                            name = "$it Ann Chan",
                            icon = AppIconCatalog.catalog[it],
                            categoryType = CategoryType.Income,
                            color = allColors[it]
                        )
                    )
                }
            }
        }
        SelectCategoryScreen(
            income = categories,
            expense = categories,
            onBack = {},
            onValueChange = {},
            value = "",
            onCategorySelected = {}
        )
    }
}