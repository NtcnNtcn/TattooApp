package com.tattoo.studio.presentation.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DateRange
import com.tattoo.studio.presentation.components.BookingDialog
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Event
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.tattoo.studio.BuildConfig
import com.tattoo.studio.data.local.db.entity.WorkEntity
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumErrorBanner
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.components.ShimmerFeedCard
import com.tattoo.studio.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FeedScreen(
    onNavigateToWorkDetail: (Int) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val works by viewModel.works.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    var bookingWorkId by remember { mutableStateOf<Int?>(null) }
    var bookingMasterName by remember { mutableStateOf("") }
    var bookingMasterId by remember { mutableStateOf<Int?>(null) }
    
    val bookingSuccess by viewModel.bookingSuccess.collectAsState()
    val isClient = userRole?.lowercase() == "client"

    if (bookingSuccess == true) {
        LaunchedEffect(Unit) {
            bookingWorkId = null
            bookingMasterId = null
            viewModel.resetBookingStatus()
        }
    }

    Scaffold(
        topBar = { 
            MagnumTopAppBar(
                onFilterClick = { showFilterDialog = true },
                onRefreshClick = { viewModel.refreshWorks() }
            ) 
        },
        containerColor = Background
    ) { padding ->
        if (showFilterDialog) {
            FilterDialog(
                tags = tags,
                selectedTags = selectedTags,
                onTagToggle = { tag ->
                    viewModel.toggleTag(tag)
                },
                onDismiss = { 
                    showFilterDialog = false
                    viewModel.refreshWorks()
                }
            )
        }
        
        if (bookingWorkId != null && bookingMasterId != null) {
            BookingDialog(
                masterName = bookingMasterName,
                onDismiss = { 
                    bookingWorkId = null
                    bookingMasterId = null
                    viewModel.resetBookingStatus()
                },
                onConfirm = { contact, message ->
                    viewModel.createApplication(bookingMasterId!!, contact, message)
                },
                isLoading = isLoading,
                error = if (bookingSuccess == false) error?.userMessage else null
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Error banner (non-blocking — shown when we have cached data but refresh failed)
            error?.let { err ->
                if (works.isNotEmpty()) {
                    MagnumErrorBanner(
                        message = err.userMessage,
                        onDismiss = { viewModel.clearError() },
                        modifier = Modifier.padding(horizontal = Dimens.margin, vertical = 8.dp)
                    )
                }
            }
            
            // Feed Content
            if (works.isEmpty() && isLoading) {
                // Shimmer loading instead of plain spinner
                Box(modifier = Modifier.fillMaxSize()) {
                    ShimmerFeedCard()
                }
            } else if (works.isEmpty() && error != null) {
                // Full-screen error state — no cached data and network failed
                MagnumErrorState(
                    error = error!!
                )
            } else if (works.isEmpty()) {
                // Handle empty state gracefully
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.refreshWorks() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "НЕТ ДОСТУПНЫХ РАБОТ", 
                            style = MaterialTheme.typography.bodyLarge.copy(color = Outline)
                        )
                    }
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { works.size })
                
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.refreshWorks() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        val work = works[page]
                        FeedItem(
                            work = work,
                            isClient = isClient,
                            onLikeClick = { viewModel.toggleLike(work.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(work.id) },
                            onWorkClick = { onNavigateToWorkDetail(work.id) },
                            onBookClick = { 
                                bookingWorkId = work.id
                                bookingMasterId = work.masterId
                                bookingMasterName = work.masterName
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterDialog(
    tags: List<String>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Background,
            shape = AppShapes.small, // Sharp 0dp
            border = BorderStroke(1.dp, OutlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.margin)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "ФИЛЬТРЫ",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Primary,
                        letterSpacing = 4.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tags.forEach { tag ->
                        val isSelected = selectedTags.contains(tag)
                        Surface(
                            onClick = { onTagToggle(tag) },
                            color = if (isSelected) Primary else SurfaceContainerLowest.copy(alpha = 0.6f),
                                contentColor = if (isSelected) Background else OnSurfaceVariant,
                                shape = AppShapes.small,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Primary else OutlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 2.sp,
                                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else null,
                                            color = LocalContentColor.current
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    com.tattoo.studio.presentation.components.MagnumButton(
                        text = "ЗАКРЫТЬ",
                        onClick = onDismiss,
                        isSecondary = true
                    )
                }
            }
        }
}

@Composable
fun FeedItem(
    work: WorkEntity,
    isClient: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onWorkClick: () -> Unit,
    onBookClick: () -> Unit
) {
    val splitTags = work.tagNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val workTitle = splitTags.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Tattoo Art"
    val likeText = if (work.likeCount >= 1000) {
        String.format("%.1fK", work.likeCount / 1000.0)
    } else {
        work.likeCount.toString()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .clickable { onWorkClick() }
    ) {
        // Background Image with loading state
        SubcomposeAsyncImage(
            model = "${BuildConfig.BASE_URL}${work.imageUrl}",
            contentDescription = "Tattoo Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().align(Alignment.Center)
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
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (state is coil3.compose.AsyncImagePainter.State.Error) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceContainerLowest),
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
        
        // Dark Gradient Overlay — smooth from 50% of screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Background.copy(alpha = 0.3f),
                            Background.copy(alpha = 0.7f),
                            Background.copy(alpha = 0.95f)
                        ),
                        startY = 0f
                    )
                )
        )
        
        // Content Overlay
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.margin, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Info Section
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                // Artist Profile — subtle glassmorphic row
                Row(
                    modifier = Modifier
                        .background(Background.copy(alpha = 0.3f), CircleShape)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = if (work.masterAvatar != null) "${BuildConfig.BASE_URL}${work.masterAvatar}" else null,
                        contentDescription = "Master Avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, Primary.copy(alpha = 0.5f), CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(android.R.drawable.ic_menu_report_image),
                        error = painterResource(android.R.drawable.ic_menu_report_image)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = work.masterName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Title from work data
                Text(
                    text = workTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Tags - smaller and cleaner
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    splitTags.take(3).forEach { tag ->
                        Text(
                            text = "#${tag.lowercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Primary.copy(alpha = 0.8f)),
                        )
                    }
                }
            }
            
            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                ActionIcon(
                    icon = Icons.Default.FavoriteBorder,
                    activeIcon = Icons.Default.Favorite,
                    isActive = work.isLiked,
                    activeColor = Color(0xFFFF3B30),
                    text = likeText,
                    onClick = onLikeClick
                )
                ActionIcon(
                    icon = Icons.Default.BookmarkBorder,
                    activeIcon = Icons.Default.Bookmark,
                    isActive = work.isFavorited,
                    activeColor = Color(0xFFFFD60A),
                    onClick = onFavoriteClick
                )
                if (isClient) {
                    ActionIcon(
                        icon = Icons.Default.DateRange,
                        text = "ЗАПИСЬ",
                        onClick = onBookClick
                    )
                }
            }
        }
    }
}

@Composable
fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isActive: Boolean = false,
    text: String? = null,
    activeColor: Color = Primary,
    inactiveColor: Color = Primary,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isActive) {
        if (isActive) {
            scale.animateTo(1.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            scale.animateTo(1f)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceContainer.copy(alpha = 0.5f))
                .border(1.dp, if (isActive) activeColor else OutlineVariant, CircleShape)
                .clickable {
                    onClick()
                    scope.launch {
                        scale.animateTo(0.8f, animationSpec = tween(100))
                        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive && activeIcon != null) activeIcon else icon,
                contentDescription = null,
                tint = if (isActive) activeColor else inactiveColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            )
        }
        if (text != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isActive) activeColor else OnSurfaceVariant
                )
            )
        }
    }
}
