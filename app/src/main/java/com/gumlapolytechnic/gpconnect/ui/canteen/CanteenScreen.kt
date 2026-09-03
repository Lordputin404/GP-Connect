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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.ui.navigation.Routes
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.SectionHeader
import com.gumlapolytechnic.gpconnect.ui.login.SessionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel as vm
import androidx.navigation.NavHostController

/**
 * Main canteen catalog screen showing categories and available menu items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanteenScreen(
    onItemClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onBack: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: CanteenViewModel = viewModel {
        CanteenViewModel(app.container.canteenRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Cart count is exposed via the same SessionViewModel that the cart screen
    // uses, keeping a single source of truth.
    val sessionViewModel: SessionViewModel = vm {
        SessionViewModel(app.container.authRepository)
    }
    val cartState by sessionViewModel.cartState.collectAsStateWithLifecycle()
    val cartCount = cartState.totalQuantity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.canteen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCartClick) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge { Text(cartCount.toString()) }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = stringResource(R.string.canteen_cart_cd),
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate(Routes.CANTEEN_ORDERS) }) {
                        Icon(
                            imageVector = Icons.Outlined.List,
                            contentDescription = stringResource(R.string.canteen_orders_cd),
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
            // Categories
            SectionHeader(
                title = stringResource(R.string.canteen_section_categories),
                actionLabel = null,
                onActionClick = null,
            )
            when {
                state.isLoading -> {
                    // Show category shimmers using existing component
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) {
                            NoticeCardShimmer()
                        }
                    }
                }
                state.isError -> {
                    ErrorState(
                        message = stringResource(R.string.canteen_error_body),
                        onRetry = { /* Repository will retry on re-subscription */ },
                    )
                }
                state.categories.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.canteen_empty_categories_title),
                        message = stringResource(R.string.canteen_empty_categories_body),
                    )
                }
                else -> {
                    // Categories row with horizontal scrolling
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        // Add "All" chip first
                        CategoryChip(
                            label = stringResource(R.string.canteen_category_all),
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
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Menu items
            SectionHeader(
                title = stringResource(R.string.canteen_section_menu),
                actionLabel = null,
                onActionClick = null,
            )
            when {
                state.isLoading -> {
                    // Show menu item shimmers using existing component
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(6) {
                            NoticeCardShimmer()
                        }
                    }
                }
                state.isError -> {
                    ErrorState(
                        message = stringResource(R.string.canteen_error_body),
                        onRetry = { /* Repository will retry on re-subscription */ },
                    )
                }
                state.menuItems.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.canteen_empty_menu_title),
                        message = stringResource(R.string.canteen_empty_menu_body),
                    )
                }
                else -> {
                    // Display menu items
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.menuItems, key = { it.id }) { item ->
                            // Menu item card
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
                                                text = item.formattedPrice(),
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