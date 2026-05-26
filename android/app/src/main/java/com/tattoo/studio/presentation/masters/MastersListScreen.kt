package com.tattoo.studio.presentation.masters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.tattoo.studio.BuildConfig
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.theme.*

enum class MasterStatusTab(val label: String) {
    ACTIVE("ДЕЙСТВУЮЩИЕ"),
    FROZEN("ЗАМОРОЖЕННЫЕ"),
    PENDING("В ОЧЕРЕДИ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MastersListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: MastersListViewModel = hiltViewModel()
) {
    val masters by viewModel.masters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedTab by remember { mutableStateOf(MasterStatusTab.ACTIVE) }
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.loadMasters()
    }

    val filteredMasters = masters.filter {
        when (selectedTab) {
            MasterStatusTab.ACTIVE  -> it.status?.lowercase() == "active"
            MasterStatusTab.FROZEN  -> it.status?.lowercase() == "frozen"
            MasterStatusTab.PENDING -> it.status?.lowercase() == "pending" || it.status?.lowercase() == "frozen"
        }
    }

    MagnumBackground {
        Scaffold(
            topBar = { 
                MagnumTopAppBar(
                ) 
            },
            containerColor = Color.Transparent
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.loadMasters() },
                state = pullState,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Background.copy(alpha = 0.8f),
                    contentColor = Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = Primary
                        )
                    },
                    divider = {
                        HorizontalDivider(color = OutlineVariant)
                    }
                ) {
                    MasterStatusTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading && masters.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Primary
                        )
                    } else if (masters.isEmpty() && error != null) {
                        MagnumErrorState(error = error!!)
                    } else if (filteredMasters.isEmpty()) {
                        Text(
                            text = "В этом разделе нет мастеров",
                            style = MaterialTheme.typography.bodyLarge.copy(color = Outline),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Dimens.margin),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredMasters) { master ->
                                MasterItem(
                                    master = master,
                                    onClick = { onNavigateToDetail(master.id) }
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
fun MasterItem(
    master: UserDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = AppShapes.small,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.7f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!master.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = "${BuildConfig.BASE_URL}${master.avatarUrl}",
                        contentDescription = master.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = master.fullName.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Primary,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = master.email,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Outline,
                        fontSize = 11.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge
                    StatusBadge(status = master.status ?: "active")
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // ID Badge
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, OutlineVariant)
                    ) {
                        Text(
                            text = "ID: ${master.id}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Outline,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when {
        status.lowercase() == "active"   -> Primary to "АКТИВЕН"
        status.lowercase() == "frozen"   -> Color(0xFF64B5F6) to "ЗАМОРОЖЕН"
        status.lowercase() == "pending_deletion" -> Color(0xFFE57373) to "УДАЛЕНИЕ"
        else -> Color(0xFFFFB74D) to "ОЖИДАЕТ"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
