package com.gumlapolytechnic.gpconnect.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * Skeleton placeholder for a menu item card, matching the redesigned card layout.
 */
@Composable
fun MenuItemShimmer(modifier: Modifier = Modifier) {
    val shimmerAlpha by rememberInfiniteTransition(label = "menuItemShimmer")
        .animateFloat(
            initialValue = 0.4f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "shimmer-alpha",
        )
    val blockAlpha = shimmerAlpha
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Image placeholder
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(80.dp, 80.dp)
                        .alpha(blockAlpha),
                ) {}
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBlock(modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .alpha(blockAlpha)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBlock(modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .alpha(blockAlpha)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBlock(modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .alpha(blockAlpha)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Add button placeholder
                ShimmerBlock(modifier = Modifier
                    .size(36.dp, 36.dp)
                    .alpha(blockAlpha)
                )
            }
        }
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