package com.tattoo.studio.presentation.masters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.remote.dto.TattooWorkFeedDto
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDetailScreen(
    masterId: Int,
    onBack: () -> Unit,
    onNavigateToWorkDetail: (Int) -> Unit,
    viewModel: MasterDetailViewModel = hiltViewModel()
) {
    val master by viewModel.master.collectAsState()
    val works by viewModel.works.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(masterId) {
        viewModel.loadMasterData(masterId)
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
                if (isLoading && master == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                } else if (master == null && error != null) {
                    MagnumErrorState(error = error!!)
                } else if (master != null) {
                    MasterDetailContent(
                        master = master!!,
                        works = works,
                        currentUser = currentUser,
                        onStatusChange = { newStatus -> 
                            viewModel.updateMasterStatus(masterId, newStatus)
                        },
                        onWorkClick = onNavigateToWorkDetail
                    )
                }
            }
        }
    }
}

@Composable
fun MasterDetailContent(
    master: UserDto,
    works: List<TattooWorkFeedDto>,
    currentUser: UserDto?,
    onStatusChange: (String) -> Unit,
    onWorkClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.margin),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item(span = { GridItemSpan(3) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant)
                        .border(2.dp, Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!master.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = if (master.avatarUrl.startsWith("http")) master.avatarUrl else "${BuildConfig.BASE_URL}${master.avatarUrl}",
                            contentDescription = master.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = master.fullName.uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Text(
                    text = master.email,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Outline)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Badge
                StatusBadge(status = master.status ?: "active")

                Spacer(modifier = Modifier.height(24.dp))

                // Admin Actions
                if (currentUser?.role?.name?.lowercase() == "admin") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ДЕЙСТВИЯ АДМИНИСТРАТОРА",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Outline,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (master.status != "active") {
                                Button(
                                    onClick = { onStatusChange("active") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Primary.copy(alpha = 0.1f), 
                                        contentColor = Primary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                                ) {
                                    Text("АКТИВИРОВАТЬ", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (master.status != "frozen") {
                                Button(
                                    onClick = { onStatusChange("frozen") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF64B5F6).copy(alpha = 0.1f), 
                                        contentColor = Color(0xFF64B5F6)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f))
                                ) {
                                    Text("ЗАМОРОЗИТЬ", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (master.status != "pending_deletion") {
                                Button(
                                    onClick = { onStatusChange("pending_deletion") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE57373).copy(alpha = 0.1f), 
                                        contentColor = Color(0xFFE57373)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.3f))
                                ) {
                                    Text("УДАЛИТЬ", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Description
                if (!master.description.isNullOrEmpty()) {
                    Text(
                        text = "О МАСТЕРЕ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Outline,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = master.description,
                        style = MaterialTheme.typography.bodyLarge.copy(color = OnBackground),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Styles inferred from works if any
                val styles = works.flatMap { it.tags }.map { it.name }.distinct()
                if (styles.isNotEmpty()) {
                    Text(
                        text = "ОСНОВНЫЕ СТИЛИ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Outline,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styles.forEach { style ->
                            Surface(
                                color = Primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = style.uppercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    text = "ПОРТФОЛИО",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Outline,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }

        // Works Grid
        items(works) { work ->
            AsyncImage(
                model = if (work.imageUrl.startsWith("http")) work.imageUrl else "${BuildConfig.BASE_URL}${work.imageUrl}",
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceVariant)
                    .clickable { onWorkClick(work.id) },
                contentScale = ContentScale.Crop
            )
        }
        
        if (works.isEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "У мастера пока нет работ",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Outline)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
