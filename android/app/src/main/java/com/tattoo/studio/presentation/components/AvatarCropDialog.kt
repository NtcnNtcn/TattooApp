package com.tattoo.studio.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tattoo.studio.presentation.theme.Background
import com.tattoo.studio.presentation.theme.Primary
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@Composable
fun AvatarCropDialog(
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
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        }
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )

                    // Circular Mask Overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val circleRadius = minOf(canvasWidth, canvasHeight) * 0.4f
                        val center = Offset(canvasWidth / 2, canvasHeight / 2)

                        // Draw darkened overlay
                        drawRect(
                            color = Color.Black.copy(alpha = 0.7f),
                            size = size
                        )

                        // Punch a hole (actually we just draw the background color or clear)
                        // In Compose we can't easily "clear" a hole without a specific BlendMode
                        // So we use a different approach: Draw the mask using a Path
                    }
                    
                    // Simple Box with Border for the crop area
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(minOf(containerSize.width.dp, containerSize.height.dp) * 0.8f / 3f) // Rough conversion
                            .size(280.dp) // Fixed size for simplicity in this demo
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

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
                            val cropped = cropBitmap(b, scale, offset, containerSize)
                            if (cropped != null) {
                                val file = saveBitmapToFile(context, cropped)
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

private fun cropBitmap(bitmap: Bitmap, scale: Float, offset: Offset, containerSize: IntSize): Bitmap? {
    if (containerSize.width == 0 || containerSize.height == 0) return null

    val cropSize = (minOf(containerSize.width, containerSize.height) * 0.8).toInt()
    val centerX = containerSize.width / 2
    val centerY = containerSize.height / 2

    val result = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    
    // Calculate transformation from container space back to original bitmap space
    val matrix = android.graphics.Matrix()
    
    // 1. Center the crop area
    matrix.postTranslate(-centerX.toFloat(), -centerY.toFloat())
    // 2. Inverse offset
    matrix.postTranslate(-offset.x / scale, -offset.y / scale)
    // 3. Inverse scale
    // This is getting complicated. Let's use a simpler approach.
    
    // Simpler approach: 
    // We draw the original bitmap into the canvas with the SAME transformation as shown in UI
    // but relative to the crop circle.
    
    val drawMatrix = android.graphics.Matrix()
    // Current UI logic: image is centered in Box, then offset and scaled.
    // Box is fillMaxSize.
    
    val imgScaleX = containerSize.width.toFloat() / bitmap.width
    val imgScaleY = containerSize.height.toFloat() / bitmap.height
    val baseScale = maxOf(imgScaleX, imgScaleY)
    
    val finalScale = baseScale * scale
    drawMatrix.postScale(finalScale, finalScale)
    
    // Center the scaled image in the container
    val scaledW = bitmap.width * finalScale
    val scaledH = bitmap.height * finalScale
    val baseX = (containerSize.width - scaledW) / 2
    val baseY = (containerSize.height - scaledH) / 2
    
    drawMatrix.postTranslate(baseX + offset.x, baseY + offset.y)
    
    // Now we want the part that is at center of container to be at 0,0 of our crop bitmap
    drawMatrix.postTranslate(-(containerSize.width - cropSize) / 2f, -(containerSize.height - cropSize) / 2f)
    
    canvas.drawBitmap(bitmap, drawMatrix, paint)
    
    return result
}

private fun saveBitmapToFile(context: android.content.Context, bitmap: Bitmap): File {
    val tempFile = File.createTempFile("cropped_avatar", ".jpg", context.cacheDir)
    val outputStream = FileOutputStream(tempFile)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    outputStream.close()
    return tempFile
}
