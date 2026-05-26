package com.tattoo.studio.presentation.applications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.tattoo.studio.BuildConfig
import com.tattoo.studio.data.remote.dto.ConsultationApplicationDto
import com.tattoo.studio.data.remote.dto.MasterApplicationDto
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumErrorBanner
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientApplicationsScreen(
    onBack: () -> Unit,
    viewModel: MasterApplicationsViewModel = hiltViewModel()
) {
    val applications by viewModel.applications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val consultations by viewModel.consultations.collectAsState()
    val consultationsLoading by viewModel.consultationsLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ЗАЯВКИ МАСТЕРОВ", "КОНСУЛЬТАЦИИ")
    val isRefreshing = if (selectedTab == 0) isLoading else consultationsLoading
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.load()
        viewModel.loadConsultations()
    }

    MagnumBackground {
        Scaffold(
            topBar = { MagnumTopAppBar() },
            containerColor = Color.Transparent
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.load(); viewModel.loadConsultations() },
                state = pullState,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Background.copy(alpha = 0.8f),
                    contentColor = Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Primary
                        )
                    },
                    divider = { HorizontalDivider(color = OutlineVariant) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        )
                    }
                }

                actionError?.let { err ->
                    MagnumErrorBanner(
                        message = err.userMessage,
                        onDismiss = { viewModel.clearActionError() },
                        modifier = Modifier.padding(horizontal = Dimens.margin, vertical = 8.dp)
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> when {
                            isLoading && applications.isEmpty() -> CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center), color = Primary
                            )
                            error != null && applications.isEmpty() -> MagnumErrorState(error = error!!)
                            applications.isEmpty() -> Text(
                                text = "Нет заявок на рассмотрении",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Outline),
                                modifier = Modifier.align(Alignment.Center)
                            )
                            else -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(Dimens.margin),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(applications, key = { it.id }) { app ->
                                    MasterApplicationCard(
                                        application = app,
                                        onApprove = { viewModel.approve(app.id) },
                                        onReject = { viewModel.reject(app.id) }
                                    )
                                }
                            }
                        }
                        1 -> when {
                            consultationsLoading && consultations.isEmpty() -> CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center), color = Primary
                            )
                            consultations.isEmpty() -> Text(
                                text = "Нет заявок на консультацию",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Outline),
                                modifier = Modifier.align(Alignment.Center)
                            )
                            else -> {
                                val openConsultations = consultations.filter { it.status.lowercase() != "completed" }
                                if (openConsultations.isEmpty()) {
                                    Text(
                                        text = "Нет заявок на консультацию",
                                        style = MaterialTheme.typography.bodyLarge.copy(color = Outline),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(Dimens.margin),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(openConsultations, key = { it.id }) { app ->
                                            ConsultationApplicationCard(
                                                application = app,
                                                onClose = { viewModel.closeConsultation(app.id) }
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
        }
    }
}

@Composable
private fun MasterApplicationCard(
    application: MasterApplicationDto,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.small,
        border = BorderStroke(1.dp, OutlineVariant),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(Dimens.margin)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant)
                        .border(1.dp, Primary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!application.master.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = "${BuildConfig.BASE_URL}${application.master.avatarUrl}",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Outline)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.master.fullName.uppercase(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = application.master.email,
                        style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = OutlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Primary
                    ),
                    border = BorderStroke(1.dp, OutlineVariant)
                ) {
                    Text(
                        "ОДОБРИТЬ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            color = LocalContentColor.current
                        )
                    )
                }

                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerHighest,
                        contentColor = Primary
                    ),
                    border = BorderStroke(1.dp, OutlineVariant)
                ) {
                    Text(
                        "ОТКЛОНИТЬ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            color = LocalContentColor.current
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsultationApplicationCard(
    application: ConsultationApplicationDto,
    onClose: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isClosed = application.status.lowercase() == "completed"
    val statusColor = when (application.status.lowercase()) {
        "pending"   -> Color(0xFFFFB74D)
        "accepted"  -> Primary
        "rejected"  -> MaterialTheme.colorScheme.error
        "completed" -> Outline
        else        -> Outline
    }
    val statusText = when (application.status.lowercase()) {
        "pending"   -> "ОЖИДАЕТ"
        "accepted"  -> "ПРИНЯТА"
        "rejected"  -> "ОТКЛОНЕНА"
        "completed" -> "ЗАВЕРШЕНА"
        else        -> application.status.uppercase()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.small,
        border = BorderStroke(1.dp, OutlineVariant),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(Dimens.margin)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ЗАЯВКА #${application.id}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Outline,
                        letterSpacing = 1.sp
                    )
                )
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = OutlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarIcon(avatarUrl = application.client.avatarUrl)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = application.client.fullName.uppercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = application.client.email,
                        style = MaterialTheme.typography.labelSmall.copy(color = Outline)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "→ ${application.master.fullName}",
                style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant)
            )

            if (!application.contactInfo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = application.contactInfo,
                        style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("contact", application.contactInfo))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Копировать",
                            tint = Outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!application.message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${application.message}\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Outline,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }

            if (!isClosed) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerHighest,
                        contentColor = Primary
                    ),
                    border = BorderStroke(1.dp, OutlineVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        "ЗАКРЫТЬ ЗАЯВКУ",
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

@Composable
private fun AvatarIcon(avatarUrl: String?) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SurfaceVariant)
            .border(1.dp, Primary.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = "${BuildConfig.BASE_URL}$avatarUrl",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Person, contentDescription = null, tint = Outline, modifier = Modifier.size(20.dp))
        }
    }
}
