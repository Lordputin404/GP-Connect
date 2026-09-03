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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.login.SessionViewModel

/**
 * Detail screen for a canteen menu item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanteenItemDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    sessionViewModel: SessionViewModel,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: CanteenViewModel = viewModel {
        CanteenViewModel(app.container.canteenRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by sessionViewModel.cartState.collectAsStateWithLifecycle()
    val cartCount = cartState.totalQuantity

    // Find the item in the catalog state
    val item = state.menuItems.firstOrNull { it.id == itemId }
    val isUnavailable = item != null && !item.isAvailable
    val inCartQuantity = cartState.quantityOf(itemId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.canteen_item_detail_title)) },
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
            Spacer(modifier = Modifier.height(24.dp))

            when {
                state.isLoading -> {
                    // Show loading shimmer using existing component
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
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
                item == null -> {
                    // Item not found
                    EmptyState(
                        title = stringResource(R.string.canteen_item_not_found_title),
                        message = stringResource(R.string.canteen_item_not_found_body),
                    )
                }
                else -> {
                    // Display the item details
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Item image placeholder
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Restaurant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(64.dp),
                                )
                            }
                        }

                        // Item details
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = item.formattedPrice(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (item.isAvailable) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Text(
                                        text = if (item.isAvailable) stringResource(R.string.canteen_item_available) else stringResource(R.string.canteen_item_unavailable),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (item.isAvailable) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))

                            if (inCartQuantity > 0) {
                                Text(
                                    text = stringResource(R.string.canteen_in_cart_format, inCartQuantity),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = { item?.let { sessionViewModel.addToCart(it) } },
                                enabled = item != null && item.isAvailable,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = when {
                                        item == null -> stringResource(R.string.canteen_add_to_cart)
                                        isUnavailable -> stringResource(R.string.canteen_item_unavailable)
                                        else -> stringResource(R.string.canteen_add_to_cart)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}