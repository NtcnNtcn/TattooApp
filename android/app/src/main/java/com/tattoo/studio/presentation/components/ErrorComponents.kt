package com.tattoo.studio.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tattoo.studio.data.remote.AppError
import com.tattoo.studio.presentation.theme.*

/**
 * Full-screen empty/error state — used when there is no data at all
 * and the initial load failed.
 */
@Composable
fun MagnumErrorState(
    error: AppError,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (error) {
        is AppError.Network, is AppError.Timeout -> Icons.Default.WifiOff
        else -> Icons.Default.Warning
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.margin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Outline
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = error.userMessage,
            style = MaterialTheme.typography.bodyLarge.copy(color = OnSurfaceVariant),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
    }
}

/**
 * Compact error banner that sits at the top/bottom of a screen.
 * Good for non-blocking errors that overlay existing content.
 */
@Composable
fun MagnumErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SecondaryContainer.copy(alpha = 0.15f))
            .border(1.dp, SecondaryContainer.copy(alpha = 0.4f), AppShapes.small)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Закрыть",
                tint = Outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Shows a Snackbar-style notification for an error.
 * Attach to a SnackbarHostState in the Scaffold.
 */
suspend fun showErrorSnackbar(
    snackbarHostState: SnackbarHostState,
    error: AppError
): SnackbarResult {
    return snackbarHostState.showSnackbar(
        message = error.userMessage,
        duration = SnackbarDuration.Long
    )
}
