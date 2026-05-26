package com.tattoo.studio.presentation.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.SubcomposeAsyncImage
import com.tattoo.studio.data.remote.dto.SubscriptionDto
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.components.ShimmerListItem
import com.tattoo.studio.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SocialViewModel = hiltViewModel()
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSubscriptions()
    }

    MagnumBackground {
        Scaffold(
            topBar = { MagnumTopAppBar() },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.margin, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "ВАШИ ПОДПИСКИ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${subscriptions.size} МАСТЕРА",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Outline,
                            letterSpacing = 1.sp
                        )
                    )
                }
                
                HorizontalDivider(color = OutlineVariant, thickness = 1.dp)

                if (subscriptions.isEmpty() && isLoading) {
                    Column {
                        repeat(3) {
                            ShimmerListItem()
                        }
                    }
                } else if (subscriptions.isEmpty() && error != null) {
                    MagnumErrorState(
                        error = error!!
                    )
                } else if (subscriptions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет подписок",
                            style = MaterialTheme.typography.bodyLarge.copy(color = Outline)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Dimens.margin, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(subscriptions) { sub ->
                            SubscriptionCard(sub = sub)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(sub: SubscriptionDto) {
    // Mock data for rich card display — in production, fetch from master endpoint
    val mockNames = listOf("АЛЕКСАНДР ВОЛКОВ", "ЕЛЕНА МОРР", "ДМИТРИЙ КРЕСТ", "АННА ТЕНЬ", "ВИКТОР ШТОРМ")
    val mockStyles = listOf("BLACKWORK / DOTWORK", "DARK LETTERING / CHICANO", "REALISM / HORROR", "LINEWORK / MINIMALISM", "JAPANESE / NEOTRADITIONAL")
    val mockRatings = listOf("4.9 (120)", "5.0 (85)", "4.8 (210)", "4.7 (65)", "4.6 (145)")
    
    val index = (sub.masterId - 1).coerceIn(0, mockNames.lastIndex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = AppShapes.small,
        border = BorderStroke(1.dp, OutlineVariant),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Master photo placeholder — dark gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SurfaceContainerHigh,
                                SurfaceContainerLowest
                            )
                        )
                    )
            )
            
            // Avatar circle overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHighest)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
            )
            
            // Gradient overlay from bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Background.copy(alpha = 0.7f),
                                Background.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            
            // Name and styles overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = Dimens.margin, bottom = 56.dp)
            ) {
                Text(
                    text = mockNames[index],
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mockStyles[index],
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Outline,
                        letterSpacing = 1.sp
                    )
                )
            }
            
            // Bottom bar — rating + profile button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(SurfaceContainerLowest.copy(alpha = 0.6f))
                    .padding(horizontal = Dimens.margin, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = mockRatings[index],
                        style = MaterialTheme.typography.labelSmall.copy(color = Outline)
                    )
                }
                
                Button(
                    onClick = { },
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Background
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "ПРОФИЛЬ",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp)
                    )
                }
            }
        }
    }
}
