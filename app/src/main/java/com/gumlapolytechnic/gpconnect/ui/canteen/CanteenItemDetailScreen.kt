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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.login.SessionViewModel
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.ButtonDefaults

/**
 * Detail screen for a canteen menu item.
 * Visually aligned with the redesigned catalog cards.
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
                            .padding(horizontal = 16.dp),
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
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item == null -> {
                    // Item not found
                    EmptyState(
                        title = stringResource(R.string.canteen_item_not_found_title),
                        message = stringResource(R.string.canteen_item_not_found_body),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                else -> {
                    // Display the item details with redesigned layout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Item Image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(MaterialTheme.shapes.medium),
                        ) {
                            if (item.imageUrl != null && item.imageUrl!!.isNotBlank()) {
                                AsyncImage(
                                    model = item.imageUrl!!,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Restaurant,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .align(Alignment.Center),
                                    )
                                }
                            }
                        }

                        // Item Details
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
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
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                shape = MaterialTheme.shapes.medium,
                                colors = if (item != null && item.isAvailable) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            ) {
                                Text(
                                    text = when {
                                        item == null -> stringResource(R.string.canteen_add_to_cart)
                                        isUnavailable -> stringResource(R.string.canteen_item_unavailable)
                                        else -> stringResource(R.string.canteen_add_to_cart)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}