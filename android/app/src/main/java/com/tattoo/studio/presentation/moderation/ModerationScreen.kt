package com.tattoo.studio.presentation.moderation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.tattoo.studio.BuildConfig
import com.tattoo.studio.data.remote.dto.TattooWorkOutDto
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.components.MagnumButton
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumErrorBanner
import com.tattoo.studio.presentation.theme.Dimens
import com.tattoo.studio.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
    viewModel: ModerationViewModel = hiltViewModel()
) {
    val pendingWorks by viewModel.pendingWorks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.loadPendingWorks()
    }

    MagnumBackground {
        Scaffold(
            topBar = { MagnumTopAppBar() },
            containerColor = Color.Transparent
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.loadPendingWorks() },
                state = pullState,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header — matching mockup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.margin, vertical = 16.dp)
                ) {
                    Text(
                        text = "МОДЕРАЦИЯ РАБОТ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ожидают проверки: ${pendingWorks.size}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
                    )
                }
                
                HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
                
                // Action error banner (approve/reject failed)
                actionError?.let { err ->
                    MagnumErrorBanner(
                        message = err.userMessage,
                        onDismiss = { viewModel.clearActionError() },
                        modifier = Modifier.padding(horizontal = Dimens.margin, vertical = 8.dp)
                    )
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    if (pendingWorks.isEmpty() && isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                    } else if (pendingWorks.isEmpty() && error != null) {
                        MagnumErrorState(
                            error = error!!
                        )
                    } else if (pendingWorks.isEmpty()) {
                        Text(
                            text = "Нет работ на модерации", 
                            style = MaterialTheme.typography.bodyLarge.copy(color = Outline),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Dimens.margin),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            items(pendingWorks) { work ->
                                PendingWorkCard(
                                    work = work,
                                    onApprove = { viewModel.approve(work.id) },
                                    onReject = { reason -> viewModel.reject(work.id, reason) }
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun PendingWorkCard(
    work: TattooWorkOutDto,
    onApprove: () -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.small,
        border = BorderStroke(1.dp, OutlineVariant),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.7f))
    ) {
        Column {
            // Image — NO blur, clear display
            Box(modifier = Modifier.fillMaxWidth()) {
                SubcomposeAsyncImage(
                    model = "${BuildConfig.BASE_URL}${work.imageUrl}",
                    contentDescription = "Pending Work",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                ) {
                    val state = painter.state
                    if (state is coil3.compose.AsyncImagePainter.State.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SurfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (state is coil3.compose.AsyncImagePainter.State.Error) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SurfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Outline
                            )
                        }
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
                
                // Gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Background.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
                
                // Master name and work title overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Dimens.margin)
                ) {
                    Text(
                        text = "МАСТЕР: ${work.description?.take(20)?.uppercase() ?: "НЕИЗВЕСТЕН"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Outline, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = work.tags.joinToString(" / ") { it.name },
                        style = MaterialTheme.typography.headlineMedium.copy(color = Primary)
                    )
                }
            }
            
            Column(modifier = Modifier.padding(Dimens.margin)) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action buttons with icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Approve button
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = AppShapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Primary
                        ),
                        border = BorderStroke(1.dp, OutlineVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "ОДОБРИТЬ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LocalContentColor.current
                            ),
                            maxLines = 1
                        )
                    }
                    
                    // Reject button
                    Button(
                        onClick = { onReject("") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = AppShapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerHighest,
                            contentColor = Primary
                        ),
                        border = BorderStroke(1.dp, OutlineVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "ОТКЛОНИТЬ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LocalContentColor.current
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
