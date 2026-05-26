package com.tattoo.studio.presentation.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.rememberAsyncImagePainter
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.components.MagnumButton
import com.tattoo.studio.presentation.components.MagnumTextField
import com.tattoo.studio.presentation.components.WorkCropDialog
import com.tattoo.studio.presentation.theme.Dimens
import com.tattoo.studio.presentation.theme.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWorkScreen(
    onUploadSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var croppedFile by remember { mutableStateOf<File?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            croppedFile = null
            showCropDialog = true
        }
    }

    if (showCropDialog && imageUri != null) {
        WorkCropDialog(
            uri = imageUri!!,
            onDismiss = {
                showCropDialog = false
                if (croppedFile == null) imageUri = null
            },
            onResult = { file ->
                croppedFile = file
                showCropDialog = false
            }
        )
    }

    MagnumBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { MagnumTopAppBar(onMenuClick = onBack) },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Dimens.margin)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                error?.let { err ->
                    com.tattoo.studio.presentation.components.MagnumErrorBanner(
                        message = err.userMessage,
                        onDismiss = { viewModel.clearError() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Text(
                    text = "НОВАЯ РАБОТА",
                    style = MaterialTheme.typography.headlineMedium.copy(color = Primary),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Загрузите фотографию вашей работы для публикации в портфолио.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Image Upload Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .border(1.dp, OutlineVariant, AppShapes.small)
                        .background(SurfaceContainerLowest.copy(alpha = 0.5f))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (croppedFile != null) {
                        Image(
                            painter = rememberAsyncImagePainter(croppedFile),
                            contentDescription = "Cropped Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Re-crop hint overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.0f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = "Нажмите, чтобы выбрать другое фото",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 8.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Image",
                                modifier = Modifier.size(48.dp),
                                tint = Primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "ВЫБРАТЬ ФОТО",
                                style = MaterialTheme.typography.labelLarge.copy(color = Primary, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Формат 4:5 · до 10 МБ",
                                style = MaterialTheme.typography.labelSmall.copy(color = Outline)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                MagnumTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "ОПИСАНИЕ (ОПЦИОНАЛЬНО)",
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                MagnumTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = "ТЕГИ (ЧЕРЕЗ ЗАПЯТУЮ)"
                )
                
                Text(
                    text = "Пример: реализм, ч/б, графика",
                    style = MaterialTheme.typography.labelSmall.copy(color = Outline),
                    modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 4.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                MagnumButton(
                    text = if (isLoading) "ЗАГРУЗКА..." else "ОПУБЛИКОВАТЬ",
                    onClick = {
                        val file = croppedFile
                        if (file != null) {
                            scope.launch {
                                if (viewModel.uploadWork(file, description, tags, "image/jpeg")) {
                                    onUploadSuccess()
                                }
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Пожалуйста, выберите и обрежьте изображение") }
                        }
                    },
                    enabled = !isLoading && croppedFile != null,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }
}

// Utility to convert Uri to File temporarily
fun getFileFromUri(context: android.content.Context, uri: Uri, extension: String): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload", ".$extension", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
