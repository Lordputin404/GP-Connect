package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import com.gumlapolytechnic.gpconnect.ui.components.MenuItemShimmer

/**
 * Main canteen catalog screen showing categories and available menu items.
 * Redesigned with proper margins, attractive category cards, and modern menu item cards.
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
            // Categories Section
            SectionHeader(
                title = stringResource(R.string.canteen_section_categories),
                actionLabel = null,
                onActionClick = null,
            )
            when {
                state.isLoading -> {
                    // Category shimmers
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(3) {
                            CategoryChipShimmer()
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
                state.categories.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.canteen_empty_categories_title),
                        message = stringResource(R.string.canteen_empty_categories_body),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                else -> {
                    // Categories row with horizontal scrolling
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
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

            // Menu Items Section
            SectionHeader(
                title = stringResource(R.string.canteen_section_menu),
                actionLabel = null,
                onActionClick = null,
            )
            when {
                state.isLoading -> {
                    // Menu item shimmers matching new card design
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(6) {
                            MenuItemShimmer()
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
                state.menuItems.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.canteen_empty_menu_title),
                        message = stringResource(R.string.canteen_empty_menu_body),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                else -> {
                    // Display menu items as proper cards
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.menuItems, key = { it.id }) { item ->
                            MenuItemCard(
                                item = item,
                                onClick = { onItemClick(item.id) },
                                onAddToCart = { sessionViewModel.addToCart(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: CanteenMenuItem,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
) {
    val isAvailable = item.isAvailable
    val inCartQuantity = 0 // This would need cart state, but we keep it simple for the card

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Item Image
            Box(
                modifier = Modifier
                    .size(80.dp, 80.dp)
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
                                .size(28.dp)
                                .align(Alignment.Center),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Item Details
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Availability badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isAvailable)
                            MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = if (isAvailable)
                                stringResource(R.string.canteen_item_available)
                                else stringResource(R.string.canteen_item_unavailable),
                            style = MaterialTheme.typography.labelSmall,
color = if (isAvailable)
                                MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    // Price and Add button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = item.formattedPrice(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Button(
                            onClick = onAddToCart,
                            enabled = isAvailable,
                            modifier = Modifier.padding(start = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = if (isAvailable) {
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
                                text = stringResource(R.string.canteen_add_to_cart),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChipShimmer() {
    val blockAlpha by rememberInfiniteTransition(label = "categoryChipShimmer").animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer-alpha",
    )
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(40.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        ShimmerBlock(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(blockAlpha)
        )
    }
}

@Composable
private fun ShimmerBlock(modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {}
}
