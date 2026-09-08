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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.gumlapolytechnic.gpconnect.ui.components.SectionHeader
import com.gumlapolytechnic.gpconnect.ui.canteen.OrderLineRow
import com.gumlapolytechnic.gpconnect.data.model.CanteenOrder
import com.gumlapolytechnic.gpconnect.ui.canteen.OrderStatusBadge

@Composable
private fun CardHeader(order: CanteenOrder) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.canteen_order_detail_status_format, order.status.name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    style = MaterialTheme.typography.headlineSmall,
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.canteen_order_detail_created_format, formatDate(order.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (order.updatedAt != order.createdAt) {
                    Text(
                        text = stringResource(R.string.canteen_order_detail_updated_format, formatDate(order.updatedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (order.decidedBy != null && order.decidedBy!!.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Decided by: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CardFooter(order: CanteenOrder) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.canteen_order_detail_total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = order.formattedTotal(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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



/**
 * Detail screen for a student canteen order.
 * Shows realtime status, historical items from snapshots, and allows cancellation while PENDING.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanteenOrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: CanteenOrderDetailViewModel = viewModel {
        CanteenOrderDetailViewModel(
            orderRepository = app.container.orderRepository,
            orderId = orderId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val showCancelDialog = remember { mutableStateOf<Boolean>(false) }
    val cancelReason = remember { mutableStateOf<String>("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.canteen_order_detail_title)) },
                navigationIcon = {
                    if (!state.isCancelling && !showCancelDialog.value) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back),
                            )
                        }
                    }
                },
                actions = {
                    if (state.order?.isPending == true && !state.isCancelling && !showCancelDialog.value) {
                        IconButton(
                            onClick = { showCancelDialog.value = true },
                            enabled = !state.isCancelling,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.canteen_order_detail_cancel_button),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                // Loading shimmers
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.canteen_checkout_placing),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.isError -> {
                ErrorState(
                    message = stringResource(R.string.canteen_orders_error_body),
                    onRetry = { /* Repository will retry on re-subscription */ },
                )
            }
            state.order == null -> {
                EmptyState(
                    title = stringResource(R.string.canteen_order_detail_not_found_title),
                    message = stringResource(R.string.canteen_order_detail_not_found_body),
                )
            }
            else -> {
                val order = state.order!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    // Header with status and order info
                    CardHeader(order = order)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Items from snapshots
                    SectionHeader(
                        title = stringResource(R.string.canteen_order_detail_items),
                        actionLabel = null,
                        onActionClick = null,
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(order.items, key = { it.menuItemId }) { snapshot ->
                            OrderLineRow(snapshot = snapshot)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Totals and metadata
                    CardFooter(order = order)

                    // Cancellation dialog trigger
                    if (state.cancellationError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = state.cancellationError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    // Cancel confirmation dialog
    if (showCancelDialog.value) {
        AlertDialog(
            onDismissRequest = { showCancelDialog.value = false },
            title = { Text(stringResource(R.string.canteen_order_detail_cancel_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.canteen_order_detail_cancel_dialog_body))
                    TextField(
                        value = cancelReason.value,
                        onValueChange = { cancelReason.value = it },
                        label = { Text(stringResource(R.string.canteen_order_detail_cancel_reason_label)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOrder(cancelReason.value.takeIf { it.isNotBlank() })
                        showCancelDialog.value = false
                        cancelReason.value = ""
                    },
                    enabled = !state.isCancelling,
                ) {
                    if (state.isCancelling) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.canteen_order_detail_cancel_dialog_confirm))
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCancelDialog.value = false }) {
                    Text(stringResource(R.string.canteen_order_detail_cancel_dialog_dismiss))
                }
            },
        )
    }
}
@Composable
private fun formatDate(epochMillis: Long): String {
    val date = java.util.Date(epochMillis)
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault())
    return sdf.format(date)
}



