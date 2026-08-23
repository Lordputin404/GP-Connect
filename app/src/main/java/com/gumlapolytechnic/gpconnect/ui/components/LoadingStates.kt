package com.gumlapolytechnic.gpconnect.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
 * Subtle loading placeholders built from pulsing surfaces — no third-party
 * shimmer dependency, a gentle alpha pulse only.
 */
@Composable
private fun shimmerAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer-alpha",
    )
    return alpha
}

@Composable
private fun ShimmerBlock(modifier: Modifier) {
    val blockAlpha = shimmerAlpha()
    Surface(
        modifier = modifier.alpha(blockAlpha),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {}
}

/** Skeleton placeholder for a notice card, used while notices load. */
@Composable
fun NoticeCardShimmer(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBlock(modifier = Modifier.size(width = 72.dp, height = 22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                ShimmerBlock(modifier = Modifier.size(width = 28.dp, height = 14.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.85f).height(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.6f).height(18.dp))
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBlock(modifier = Modifier.size(width = 96.dp, height = 14.dp))
        }
    }
}

/** Skeleton placeholder for the small event cards on the Home dashboard. */
@Composable
fun EventPreviewShimmer(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBlock(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                ShimmerBlock(modifier = Modifier.size(width = 150.dp, height = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBlock(modifier = Modifier.size(width = 100.dp, height = 12.dp))
            }
        }
    }
}
