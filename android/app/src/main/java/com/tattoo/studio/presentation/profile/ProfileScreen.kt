package com.tattoo.studio.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.tattoo.studio.BuildConfig
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.theme.*
import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.tattoo.studio.presentation.components.MagnumButton
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutSuccess: () -> Unit,
    onNavigateToModeration: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToMasters: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState(initial = false)
    val adminStats by viewModel.adminStats.collectAsState()
    val context = LocalContext.current
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            showCropDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    MagnumBackground {
        Scaffold(
            topBar = { MagnumTopAppBar() },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading && userProfile == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                } else if (userProfile == null && error != null) {
                    MagnumErrorState(
                        error = error!!
                    )
                } else {
                    userProfile?.let { user ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Dimens.margin, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(128.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, OutlineVariant, CircleShape)
                                        .background(SurfaceContainerHighest)
                                        .padding(4.dp)
                                        .shadow(8.dp, CircleShape)
                                ) {
                                    if (!user.avatarUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = "${BuildConfig.BASE_URL}${user.avatarUrl}",
                                            contentDescription = "Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(SurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Default Avatar",
                                                modifier = Modifier.size(64.dp),
                                                tint = OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                // Edit button
                                IconButton(
                                    onClick = { launcher.launch("image/*") },
                                    enabled = !isOffline,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(36.dp)
                                        .background(
                                            if (isOffline) SurfaceVariant else Background, 
                                            CircleShape
                                        )
                                        .border(1.dp, OutlineVariant, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = if (isOffline) OnSurfaceVariant else OnSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // User Info
                            Text(
                                text = user.fullName, 
                                style = MaterialTheme.typography.headlineMedium.copy(color = Primary),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            MagnumButton(
                                text = "управление аккаунтом",
                                onClick = onNavigateToEditProfile,
                                enabled = !isOffline,
                                isSecondary = true,
                                modifier = Modifier
                                    .width(200.dp)
                                    .padding(bottom = 24.dp)
                            )
                            
                            if (isOffline) {
                                Text(
                                    text = "ОФФЛАЙН РЕЖИМ (ФУНКЦИИ ОГРАНИЧЕНЫ)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Error,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            ) {
                                val isAdmin = user.role.name == "admin"
                                val isMaster = user.role.name == "master"
                                if (isAdmin) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatItem(
                                            value = (adminStats?.worksApproved ?: 0).toString(),
                                            label = "ПРИНЯТО РАБОТ",
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatItem(
                                            value = (adminStats?.worksRejected ?: 0).toString(),
                                            label = "ОТКЛОНЕНО РАБОТ",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatItem(
                                            value = (adminStats?.consultationsProcessed ?: 0).toString(),
                                            label = "ЗАЯВОК КЛИЕНТОВ ОБРАБОТАНО",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else if (isMaster) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatItem(
                                            value = (user.stats?.likesGiven ?: 0).toString(), 
                                            label = "ПОСТАВЛЕНО ЛАЙКОВ", 
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatItem(
                                            value = (user.stats?.favoritesCount ?: 0).toString(), 
                                            label = "В ИЗБРАННОМ", 
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatItem(
                                            value = (user.stats?.likesReceived ?: 0).toString(), 
                                            label = "ПОЛУЧЕНО ЛАЙКОВ", 
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatItem(
                                            value = (user.stats?.favoritesReceived ?: 0).toString(), 
                                            label = "В ИЗБРАННОМ У ДРУГИХ", 
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatItem(
                                            value = (user.stats?.worksCount ?: 0).toString(), 
                                            label = "РАБОТ В ПОРТФОЛИО", 
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatItem(
                                            value = (user.stats?.likesGiven ?: 0).toString(), 
                                            label = "ПОСТАВЛЕНО ЛАЙКОВ", 
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatItem(
                                            value = (user.stats?.favoritesCount ?: 0).toString(), 
                                            label = "В ИЗБРАННОМ", 
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = OutlineVariant, thickness = 1.dp)

                            Spacer(modifier = Modifier.weight(1f))

                            // Logout Button — fixed height/padding
                            Button(
                                onClick = { viewModel.logout(onComplete = onLogoutSuccess) },
                                enabled = !isOffline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = AppShapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = if (isOffline) OnSurfaceVariant else OnSurface,
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = OnSurfaceVariant.copy(alpha = 0.38f)
                                ),
                                border = BorderStroke(1.dp, if (isOffline) OutlineVariant else Outline)
                            ) {
                                Text(
                                    text = "ВЫЙТИ",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        if (showCropDialog && selectedUri != null) {
            com.tattoo.studio.presentation.components.AvatarCropDialog(
                uri = selectedUri!!,
                onDismiss = { showCropDialog = false },
                onResult = { file ->
                    showCropDialog = false
                    viewModel.uploadAvatar(file, "image/jpeg")
                }
            )
        }
    }
}

@Composable
fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(color = Primary)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = OnSurfaceVariant,
                letterSpacing = 2.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AdminActionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = AppShapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceContainer,
            contentColor = Primary
        ),
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp)
        )
    }
}

// Utility to convert Uri to File temporarily
fun getFileFromUri(context: android.content.Context, uri: Uri, extension: String): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("avatar", ".$extension", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
