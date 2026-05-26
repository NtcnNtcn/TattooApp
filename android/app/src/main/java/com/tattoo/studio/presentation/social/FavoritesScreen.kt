package com.tattoo.studio.presentation.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.SubcomposeAsyncImage
import com.tattoo.studio.BuildConfig
import com.tattoo.studio.data.remote.dto.TattooWorkFeedDto
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.theme.Dimens
import com.tattoo.studio.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onWorkClick: (Int) -> Unit,
    viewModel: SocialViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    val likes by viewModel.likes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSocialData()
    }
    
    // Filter tabs: ВСЕ / ЛАЙКИ / ИЗБРАННЫЕ
    val filterTabs = listOf("ВСЕ", "ЛАЙКИ", "ИЗБРАННЫЕ")
    var selectedTab by remember { mutableStateOf(filterTabs[0]) }

    val displayWorks = when (selectedTab) {
        "ЛАЙКИ" -> likes
        "ИЗБРАННЫЕ" -> favorites
        else -> (likes + favorites).distinctBy { it.id }
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
                    .padding(horizontal = Dimens.margin)
            ) {

                // Filter tabs — underline style matching mockup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        filterTabs.forEach { tab ->
                            val isSelected = tab == selectedTab
                            Column(
                                modifier = Modifier
                                    .clickable { selectedTab = tab }
                                    .padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = tab,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Primary else Outline,
                                        letterSpacing = 1.sp
                                    )
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(IntrinsicSize.Max)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Primary)
                                    )
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = OutlineVariant, thickness = 1.dp, modifier = Modifier.padding(bottom = 2.dp))

                if (displayWorks.isEmpty() && isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (displayWorks.isEmpty() && error != null) {
                    MagnumErrorState(
                        error = error!!
                    )
                } else if (displayWorks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет работ в этой категории", style = MaterialTheme.typography.bodyLarge.copy(color = Outline))
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalItemSpacing = 2.dp
                    ) {
                        items(displayWorks) { work ->
                            FavoriteWorkCard(
                                work = work,
                                onWorkClick = onWorkClick,
                                onLikeClick = { viewModel.toggleLike(work.id) },
                                onFavoriteClick = { viewModel.toggleFavorite(work.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteWorkCard(
    work: TattooWorkFeedDto,
    onWorkClick: (Int) -> Unit,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        SubcomposeAsyncImage(
            model = "${BuildConfig.BASE_URL}${work.imageUrl}",
            contentDescription = "Favorite Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clickable { onWorkClick(work.id) },
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(SurfaceContainerHighest)
                )
            }
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Background.copy(alpha = 0.2f), Background.copy(alpha = 0.9f)),
                        startY = 100f
                    )
                )
        )
        
        // Social Icons Row
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Like (Heart)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clickable { onLikeClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (work.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (work.isLiked) Color(0xFFFF3B30) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Favorite (Bookmark)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clickable { onFavoriteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (work.isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Favorite",
                    tint = if (work.isFavorited) Color(0xFFFFD60A) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        // Text Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = work.tags.firstOrNull()?.name?.replaceFirstChar { it.uppercase() } ?: "Tattoo Art",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp, color = Primary),
                maxLines = 1
            )
            Text(
                text = work.masterName,
                style = MaterialTheme.typography.labelSmall.copy(color = Outline),
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
