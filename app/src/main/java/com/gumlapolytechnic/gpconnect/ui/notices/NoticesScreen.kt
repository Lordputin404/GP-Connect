package com.gumlapolytechnic.gpconnect.ui.notices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCard
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.labelRes

/**
 * Notice list: search field and horizontally scrolling category chips above
 * the notice list. Search and category filter always apply together; the
 * empty state distinguishes "no matches" from "no notices".
 */
@Composable
fun NoticesScreen(onNoticeClick: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: NoticesViewModel =
        viewModel { NoticesViewModel(app.container.noticeRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.notices_screen_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.search,
            onValueChange = viewModel::onSearchChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (state.search.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            item {
                CategoryChip(
                    label = stringResource(R.string.filter_all),
                    selected = state.category == null,
                    onClick = { viewModel.onCategoryChange(null) },
                )
            }
            items(NoticeCategory.entries.toList()) { category ->
                CategoryChip(
                    label = stringResource(category.labelRes),
                    selected = state.category == category,
                    onClick = {
                        viewModel.onCategoryChange(
                            if (state.category == category) null else category,
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoading -> {
                LoadingList()
            }
            state.isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    ErrorState(
                        message = stringResource(R.string.notices_error_body),
                        onRetry = viewModel::retry,
                    )
                }
            }
            state.notices.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (state.isFiltered) {
                        EmptyState(
                            title = stringResource(R.string.notices_empty_search_title),
                            message = stringResource(R.string.notices_empty_search_body),
                        )
                    } else {
                        EmptyState(
                            title = stringResource(R.string.notices_empty_title),
                            message = stringResource(R.string.notices_empty_body),
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                ) {
                    items(state.notices, key = { it.id }) { notice ->
                        NoticeCard(
                            notice = notice,
                            isRead = notice.id in state.readIds,
                            onClick = { onNoticeClick(notice.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingList() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) {
            NoticeCardShimmer()
        }
    }
}
