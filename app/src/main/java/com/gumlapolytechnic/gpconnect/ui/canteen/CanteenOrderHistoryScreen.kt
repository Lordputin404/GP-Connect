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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
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
import com.gumlapolytechnic.gpconnect.data.firebase.FirebaseServices
import com.gumlapolytechnic.gpconnect.data.model.CanteenOrder
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer

/**
 * Screen showing the student's order history and active orders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanteenOrderHistoryScreen(
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: CanteenOrderHistoryViewModel = viewModel {
        CanteenOrderHistoryViewModel(
            orderRepository = app.container.orderRepository,
            currentUid = FirebaseServices.auth.currentUser?.uid,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.canteen_orders_title)) },
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
        ) {
            when {
                state.isLoading -> {
                    // Loading shimmers
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(6) {
                            NoticeCardShimmer()
                        }
                    }
                }
                state.isError -> {
                    ErrorState(
                        message = stringResource(R.string.canteen_orders_error_body),
                        onRetry = { /* Repository will retry on re-subscription */ },
                    )
                }
                state.activeOrders.isEmpty() && state.historicalOrders.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.canteen_orders_empty_title),
                        message = stringResource(R.string.canteen_orders_empty_body),
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Active orders section
                        if (state.activeOrders.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.canteen_orders_section_active),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                            items(state.activeOrders, key = { it.id }) { order ->
                                OrderHistoryItem(
                                    order = order,
                                    onClick = { onOrderClick(order.id) },
                                )
                            }
                        }

                        // History section
                        if (state.historicalOrders.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.canteen_orders_section_history),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                            items(state.historicalOrders, key = { it.id }) { order ->
                                OrderHistoryItem(
                                    order = order,
                                    onClick = { onOrderClick(order.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryItem(
    order: CanteenOrder,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Order #${order.id}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.canteen_order_detail_created_format, formatDate(order.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OrderStatusBadge(status = order.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = order.formattedTotal(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.canteen_order_detail_items, order.items.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (order.cancellationReason != null && order.cancellationReason!!.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.canteen_order_detail_cancellation_reason_format, order.cancellationReason!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun formatDate(epochMillis: Long): String {
    val date = java.util.Date(epochMillis)
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault())
    return sdf.format(date)
}