package com.emm.justchill.hh.shared.seetransactions

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

internal fun LazyListState.reachedBottom(buffer: Int = 1): Boolean {
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
    return lastVisibleItem?.index != 0 && lastVisibleItem?.index == layoutInfo.totalItemsCount - buffer
}

@Composable
internal fun <T> EndlessLazyColumn(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    items: List<T>,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    loadMore: () -> Unit
) {

    val reachedBottom: Boolean by remember { derivedStateOf { listState.reachedBottom() } }

    LaunchedEffect(reachedBottom) {
        if (reachedBottom) loadMore()
    }

    LazyColumn(modifier = modifier, state = listState) {
        items(
            items = items,
            key = { item: T -> itemKey(item) },
        ) { item ->
            itemContent(item)
        }
    }
}