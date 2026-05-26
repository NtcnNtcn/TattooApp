package com.tattoo.studio.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.tattoo.studio.presentation.theme.*

/**
 * Shimmer loading placeholder — Magnum Gothic dark theme.
 * Use instead of CircularProgressIndicator for content areas.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        SurfaceContainerLow,
        SurfaceContainerHigh,
        SurfaceContainerLow
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200f, translateAnim.value - 200f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Box(
        modifier = modifier.background(brush)
    )
}

/**
 * Full-width shimmer card placeholder — for feed loading states.
 */
@Composable
fun ShimmerFeedCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Image placeholder
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
        )
    }
}

/**
 * Shimmer row for list items (subscriptions, etc.)
 */
@Composable
fun ShimmerListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.margin, vertical = 12.dp)
    ) {
        // Avatar
        ShimmerBox(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
            )
        }
    }
}
