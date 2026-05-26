package com.tattoo.studio.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.tattoo.studio.BuildConfig
import com.tattoo.studio.data.remote.dto.TattooWorkDetailDto
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumTextField
import com.tattoo.studio.presentation.components.MagnumButton
import com.tattoo.studio.presentation.components.BookingDialog
import com.tattoo.studio.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkDetailScreen(
    workId: Int,
    onBack: () -> Unit,
    onNavigateToMaster: (Int) -> Unit,
    viewModel: WorkDetailViewModel = hiltViewModel()
) {
    val work by viewModel.work.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isClient = userRole?.lowercase() == "client"

    LaunchedEffect(workId) {
        viewModel.loadWork(workId)
    }

    var showBookingDialog by remember { mutableStateOf(false) }
    val bookingSuccess by viewModel.bookingSuccess.collectAsState()

    if (bookingSuccess == true) {
        LaunchedEffect(Unit) {
            showBookingDialog = false
            viewModel.resetBookingStatus()
        }
    }

    MagnumBackground {
        Scaffold(
            topBar = { 
                MagnumTopAppBar(
                    onNavigationClick = onBack
                ) 
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading && work == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                } else if (work == null && error != null) {
                    MagnumErrorState(error = error!!)
                } else if (work != null) {
                    WorkDetailContent(
                        work = work!!,
                        isOffline = isOffline,
                        isClient = isClient,
                        onMasterClick = { onNavigateToMaster(work!!.master.id) },
                        onLikeClick = { viewModel.toggleLike(workId) },
                        onFavoriteClick = { viewModel.toggleFavorite(workId) },
                        onBookClick = { showBookingDialog = true }
                    )
                }
            }
            
            if (showBookingDialog && work != null && isClient) {
                BookingDialog(
                    masterName = work!!.master.fullName,
                    onDismiss = { 
                        showBookingDialog = false
                        viewModel.resetBookingStatus()
                    },
                    onConfirm = { contact, message ->
                        viewModel.createApplication(work!!.master.id, contact, message)
                    },
                    isLoading = isLoading,
                    error = if (bookingSuccess == false) error?.message else null
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkDetailContent(
    work: TattooWorkDetailDto,
    isOffline: Boolean,
    isClient: Boolean,
    onMasterClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onBookClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Large Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f) // Vertical focus
                .background(SurfaceVariant)
        ) {
            AsyncImage(
                model = if (work.imageUrl.startsWith("http")) work.imageUrl else "${BuildConfig.BASE_URL}${work.imageUrl}",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .padding(Dimens.margin)
        ) {
            // Master Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMasterClick() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant)
                        .border(1.dp, Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!work.master.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = if (work.master.avatarUrl.startsWith("http")) work.master.avatarUrl else "${BuildConfig.BASE_URL}${work.master.avatarUrl}",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Outline)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = work.master.fullName.uppercase(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "СМОТРЕТЬ ПРОФИЛЬ",
                        style = MaterialTheme.typography.labelSmall.copy(color = Outline)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Outline
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = OutlineVariant)

            // Interaction Bar
            if (isOffline) {
                Text(
                    text = "ОФФЛАЙН РЕЖИМ (ЛАЙКИ И ЗАПИСЬ НЕДОСТУПНЫ)",
                    style = MaterialTheme.typography.labelSmall.copy(color = Error),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLikeClick,
                        enabled = !isOffline
                    ) {
                        Icon(
                            imageVector = if (work.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (work.isLiked) Primary else if (isOffline) OutlineVariant else OnBackground
                        )
                    }
                    Text(
                        text = work.likeCount.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(color = OnBackground)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    IconButton(
                        onClick = onFavoriteClick,
                        enabled = !isOffline
                    ) {
                        Icon(
                            imageVector = if (work.isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (work.isFavorited) Primary else if (isOffline) OutlineVariant else OnBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            if (!work.description.isNullOrEmpty()) {
                Text(
                    text = "ОПИСАНИЕ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Outline,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = work.description,
                    style = MaterialTheme.typography.bodyLarge.copy(color = OnBackground)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Tags
            if (work.tags.isNotEmpty()) {
                Text(
                    text = "ТЕГИ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Outline,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    work.tags.forEach { tag ->
                        Surface(
                            color = Background,
                            shape = RoundedCornerShape(2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                        ) {
                            Text(
                                text = tag.name.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Primary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Appointment Button
            if (isClient) {
                MagnumButton(
                    text = "ЗАПИСАТЬСЯ К МАСТЕРУ",
                    onClick = onBookClick,
                    enabled = !isOffline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

