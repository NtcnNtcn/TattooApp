package com.tattoo.studio.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.tattoo.studio.presentation.theme.*

@Composable
fun BookingDialog(
    masterName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
    isLoading: Boolean,
    error: String? = null
) {
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerLowest,
        shape = AppShapes.small,
        tonalElevation = 8.dp,
        title = {
            Text(
                text = "ЗАПИСЬ К МАСТЕРУ",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Вы записываетесь к мастеру ${masterName.uppercase()}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = OnSurface)
                )
                
                MagnumTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = "ВАШ КОНТАКТ (ТЕЛЕФОН/ТГ)",
                    modifier = Modifier.fillMaxWidth()
                )
                
                Column {
                    MagnumTextField(
                        value = message,
                        onValueChange = { if (it.length <= 127) message = it },
                        label = "СООБЩЕНИЕ (ОПЦИОНАЛЬНО)",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        text = "${message.length}/127",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (message.length == 127) Color.Red else Outline
                        ),
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
                
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            MagnumButton(
                text = if (isLoading) "ОТПРАВКА..." else "ПОДТВЕРДИТЬ",
                onClick = { onConfirm(contact, message.ifEmpty { null }) },
                enabled = contact.isNotBlank() && !isLoading,
                modifier = Modifier.width(160.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ОТМЕНА", color = Outline)
            }
        }
    )
}

@Composable
fun MagnumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 48.dp
) {
    val containerColor = if (isSecondary) Color.Transparent else MaterialTheme.colorScheme.primary
    val contentColor = if (isSecondary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
    val borderStroke = if (isSecondary) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = AppShapes.small, // 0.dp radius
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        border = borderStroke,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = LocalContentColor.current
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun MagnumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bottomBorderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        OutlineVariant
    }
    
    val bottomBorderWidth = if (isFocused) 2.dp else 1.dp

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            interactionSource = interactionSource,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp) // Fixed height for consistency
                        .border(
                            width = bottomBorderWidth,
                            color = bottomBorderColor,
                            shape = AppShapes.small
                        )
                        .padding(horizontal = Dimens.gutter), // Removed vertical padding as height is fixed
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(color = Outline)
                        )
                    }
                    innerTextField()
                    
                    if (trailingIcon != null) {
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            trailingIcon()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun TattooWorkCard(
    imageUrl: String,
    masterName: String,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.small, // 0.dp sharp corners
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Primary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(Dimens.gutter)
            ) {
                Column {
                    Text(
                        text = masterName.uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnumTopAppBar(
    onMenuClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Background.copy(alpha = 0.95f),
                            Background.copy(alpha = 0.7f),
                            Background.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "МАГНУМ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 6.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigationClick != null) {
                        IconButton(onClick = onNavigationClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (onRefreshClick != null) {
                        IconButton(
                            onClick = onRefreshClick,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                },
                actions = {
                    if (onFilterClick != null) {
                        IconButton(
                            onClick = onFilterClick,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (onNotificationsClick != null) {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onFilterClick == null && onNotificationsClick == null) {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Primary
                )
            )
        }
    }
}
