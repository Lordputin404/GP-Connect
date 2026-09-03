package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.SectionHeader

/**
 * Main canteen catalog screen showing categories and available menu items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanteenScreen(
    onItemClick: (String) -> Unit,
    onBack: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: CanteenViewModel = viewModel {
        CanteenViewModel(app.container.canteenRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_canteen)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
        ) {
            // Search bar
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            SectionHeader(
                title = stringResource(R.string.section_quick_access), // Reusing for now
                actionLabel = null,
                onActionClick = null,
            )
            if (state.isLoading) {
                // Show category shimmers
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        CategoryChip(
                            label = "Loading...",
                            selected = false,
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else if (state.categories.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.home_no_pinned), // Reusing for now
                    message = stringResource(R.string.home_error_body), // Reusing for now
                )
            } else {
                // Categories row with horizontal scrolling
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    // Add "All" chip first
                    CategoryChip(
                        label = stringResource(R.string.filter_all),
                        selected = state.selectedCategoryId == null,
                        onClick = {
                            viewModel.selectCategory(null)
                        },
                    )
                    state.categories.forEach { category ->
                        CategoryChip(
                            label = category.name,
                            selected = state.selectedCategoryId == category.id,
                            onClick = {
                                viewModel.selectCategory(category.id)
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Menu items
            SectionHeader(
                title = stringResource(R.string.section_recent_notices), // Reusing for now
                actionLabel = null,
                onActionClick = null,
            )
            when (state.isLoading) {
                true -> {
                    // Show menu item shimmers
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(6) {
                            // Simple placeholder for menu item shimmer
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Loading...",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Loading...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Loading...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
                false -> {
                    if (state.menuItems.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.notices_empty_title), // Reusing for now
                            message = stringResource(R.string.notices_empty_body), // Reusing for now
                        )
                    } else {
                        // For now, we'll show all menu items without category filtering
                        // TODO: Implement proper category filtering when we have separate flows
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.menuItems, key = { it.id }) { item ->
                                // Simple menu item card for now
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 1.dp,
                                    onClick = { onItemClick(item.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Image placeholder
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier
                                                .size(60.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Restaurant,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.description ?: "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                            ) {
                                                Text(
                                                    text = "₹${item.priceInr.toString().padStart(4, '0')}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}