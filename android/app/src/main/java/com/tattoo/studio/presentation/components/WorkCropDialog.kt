package com.tattoo.studio.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tattoo.studio.presentation.theme.AppShapes
import com.tattoo.studio.presentation.theme.Primary
import java.io.File
import java.io.FileOutputStream

private const val CROP_RATIO_W = 4f
private const val CROP_RATIO_H = 5f

@Composable
fun WorkCropDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onResult: (File) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { containerSize = it.size }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 10f)
                                    offset += pan
                                }
                            }
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )

                        // Dark overlay around the crop rect
                        if (containerSize != IntSize.Zero) {
                            val cropW = containerSize.width * 0.88f
                            val cropH = cropW * (CROP_RATIO_H / CROP_RATIO_W)
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val left = (size.width - cropW) / 2f
                                val top = (size.height - cropH) / 2f

                                // Top
                                drawRect(Color.Black.copy(alpha = 0.72f),
                                    topLeft = Offset.Zero,
                                    size = androidx.compose.ui.geometry.Size(size.width, top))
                                // Bottom
                                drawRect(Color.Black.copy(alpha = 0.72f),
                                    topLeft = Offset(0f, top + cropH),
                                    size = androidx.compose.ui.geometry.Size(size.width, size.height - top - cropH))
                                // Left
                                drawRect(Color.Black.copy(alpha = 0.72f),
                                    topLeft = Offset(0f, top),
                                    size = androidx.compose.ui.geometry.Size(left, cropH))
                                // Right
                                drawRect(Color.Black.copy(alpha = 0.72f),
                                    topLeft = Offset(left + cropW, top),
                                    size = androidx.compose.ui.geometry.Size(size.width - left - cropW, cropH))
                            }

                            // Crop border + corner guides
                            val cropWDp = with(androidx.compose.ui.platform.LocalDensity.current) { (containerSize.width * 0.88f).toDp() }
                            val cropHDp = cropWDp * (CROP_RATIO_H / CROP_RATIO_W)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(cropWDp, cropHDp)
                                    .border(1.dp, Color.White.copy(alpha = 0.8f), AppShapes.small)
                            ) {
                                // Corner marks
                                val cornerLen = 20.dp
                                val cornerThick = 2.dp
                                val cornerColor = Primary
                                // Top-left
                                Box(Modifier.size(cornerLen, cornerThick).align(Alignment.TopStart).background(cornerColor))
                                Box(Modifier.size(cornerThick, cornerLen).align(Alignment.TopStart).background(cornerColor))
                                // Top-right
                                Box(Modifier.size(cornerLen, cornerThick).align(Alignment.TopEnd).background(cornerColor))
                                Box(Modifier.size(cornerThick, cornerLen).align(Alignment.TopEnd).background(cornerColor))
                                // Bottom-left
                                Box(Modifier.size(cornerLen, cornerThick).align(Alignment.BottomStart).background(cornerColor))
                                Box(Modifier.size(cornerThick, cornerLen).align(Alignment.BottomStart).background(cornerColor))
                                // Bottom-right
                                Box(Modifier.size(cornerLen, cornerThick).align(Alignment.BottomEnd).background(cornerColor))
                                Box(Modifier.size(cornerThick, cornerLen).align(Alignment.BottomEnd).background(cornerColor))
                            }
                        }
                    }
                }

                // Hint
                Text(
                    text = "Сдвигайте и масштабируйте фото",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                )

                // Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ОТМЕНА", color = Color.White)
                    }
                    MagnumButton(
                        text = "СОХРАНИТЬ",
                        onClick = {
                            bitmap?.let { b ->
                                val cropped = cropWorkBitmap(b, scale, offset, containerSize)
                                if (cropped != null) {
                                    val file = saveWorkBitmapToFile(context, cropped)
                                    onResult(file)
                                }
                            }
                        },
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }
    }
}

private fun cropWorkBitmap(bitmap: Bitmap, scale: Float, offset: Offset, containerSize: IntSize): Bitmap? {
    if (containerSize.width == 0 || containerSize.height == 0) return null

    val cropW = (containerSize.width * 0.88f).toInt()
    val cropH = (cropW * (CROP_RATIO_H / CROP_RATIO_W)).toInt()

    val result = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    val imgScaleX = containerSize.width.toFloat() / bitmap.width
    val imgScaleY = containerSize.height.toFloat() / bitmap.height
    val baseScale = maxOf(imgScaleX, imgScaleY)
    val finalScale = baseScale * scale

    val scaledW = bitmap.width * finalScale
    val scaledH = bitmap.height * finalScale
    val baseX = (containerSize.width - scaledW) / 2f
    val baseY = (containerSize.height - scaledH) / 2f

    val drawMatrix = android.graphics.Matrix()
    drawMatrix.postScale(finalScale, finalScale)
    drawMatrix.postTranslate(baseX + offset.x, baseY + offset.y)
    // Shift so the crop window maps to (0,0)
    drawMatrix.postTranslate(
        -((containerSize.width - cropW) / 2f),
        -((containerSize.height - cropH) / 2f)
    )

    canvas.drawBitmap(bitmap, drawMatrix, paint)
    return result
}

private fun saveWorkBitmapToFile(context: android.content.Context, bitmap: Bitmap): File {
    val tempFile = File.createTempFile("cropped_work", ".jpg", context.cacheDir)
    FileOutputStream(tempFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return tempFile
}
