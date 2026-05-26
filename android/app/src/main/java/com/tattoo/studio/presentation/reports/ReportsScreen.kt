package com.tattoo.studio.presentation.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.components.MagnumButton
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumErrorBanner
import com.tattoo.studio.presentation.theme.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSummary()
    }

    MagnumBackground {
        Scaffold(
            topBar = { MagnumTopAppBar() },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLoading && summary == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (summary == null && error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    MagnumErrorState(
                        error = error!!
                    )
                }
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.margin, vertical = 32.dp)
            ) {
                // Error banner (non-blocking, for refresh/export errors)
                error?.let { err ->
                    MagnumErrorBanner(
                        message = err.userMessage,
                        onDismiss = { viewModel.clearError() },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                // Header Section
                Column(modifier = Modifier.padding(bottom = 40.dp)) {
                    Text(
                        text = "Статистика студии",
                        style = MaterialTheme.typography.headlineLarge.copy(color = Primary),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Анализ показателей и популярности мастеров",
                        style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = "ПЕРИОД",
                                style = MaterialTheme.typography.labelSmall.copy(color = Outline),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            // Simulate dropdown with a TextField
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, OutlineVariant, AppShapes.small)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text("Текущий месяц", style = MaterialTheme.typography.bodyMedium.copy(color = OnSurface))
                            }
                        }
                        
                        MagnumButton(
                            text = "Экспорт в Excel",
                            onClick = { },
                            isSecondary = true,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }

                // Bento Grid - We'll use a Column here since it's scrollable vertically
                Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    TopArtistsCard()
                    PopularStylesCard()
                }
            }
        }
    }
}
}

@Composable
fun TopArtistsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.small,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Популярные мастера",
                style = MaterialTheme.typography.headlineMedium.copy(color = Primary),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            HorizontalDivider(color = OutlineVariant, thickness = 1.dp, modifier = Modifier.padding(bottom = 24.dp))
            
            val mockMasters = listOf(
                Triple("Александр Волк", 45, 0.85f),
                Triple("Елена Морок", 32, 0.60f),
                Triple("Виктор Тень", 18, 0.35f)
            )
            
            mockMasters.forEachIndexed { index, master ->
                ArtistRow(name = master.first, likes = master.second, progress = master.third)
                if (index < mockMasters.size - 1) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun PopularStylesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.small,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Популярные стили",
                style = MaterialTheme.typography.headlineMedium.copy(color = Primary),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            HorizontalDivider(color = OutlineVariant, thickness = 1.dp, modifier = Modifier.padding(bottom = 20.dp))
            
            val mockStyles = listOf(
                Triple("Реализм", 42, 0.42f),
                Triple("Блэкворк", 28, 0.28f),
                Triple("Графика", 15, 0.15f),
                Triple("Япония", 10, 0.10f)
            )
            
            mockStyles.forEachIndexed { index, style ->
                StyleRow(name = style.first, likes = style.second, progress = style.third)
                if (index < mockStyles.size - 1) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun ArtistRow(name: String, likes: Int, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, OutlineVariant, CircleShape)
                .background(SurfaceContainerHighest)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge.copy(color = Primary))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = likes.toString(), style = MaterialTheme.typography.labelSmall.copy(color = Outline))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Outline, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(SurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(if (progress > 0.35f) Primary else OutlineVariant)
                )
            }
        }
    }
}

@Composable
fun StyleRow(name: String, likes: Int, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(1.dp, OutlineVariant, AppShapes.small)
                .background(SurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            // Icon placeholder
            Box(modifier = Modifier.size(24.dp).background(Outline))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp, color = Primary))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = likes.toString(), style = MaterialTheme.typography.labelSmall.copy(color = Outline))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Outline, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(SurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(if (progress > 0.15f) Primary else OutlineVariant)
                )
            }
        }
    }
}
