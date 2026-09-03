package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.OrderStatus

/**
 * Compact status badge that reflects an order's [OrderStatus].
 * Active statuses use a tonal accent; terminal statuses use neutral/error tones.
 */
@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    val (labelRes, container, content) = when (status) {
        OrderStatus.PENDING -> Triple(
            R.string.canteen_order_status_pending,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrderStatus.CONFIRMED -> Triple(
            R.string.canteen_order_status_confirmed,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        OrderStatus.PREPARING -> Triple(
            R.string.canteen_order_status_preparing,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        OrderStatus.READY -> Triple(
            R.string.canteen_order_status_ready,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        OrderStatus.COMPLETED -> Triple(
            R.string.canteen_order_status_completed,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrderStatus.CANCELLED -> Triple(
            R.string.canteen_order_status_cancelled,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}