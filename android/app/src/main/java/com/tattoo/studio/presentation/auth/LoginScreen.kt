package com.tattoo.studio.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.hilt.navigation.compose.hiltViewModel
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumButton
import com.tattoo.studio.presentation.components.MagnumErrorBanner
import com.tattoo.studio.presentation.components.MagnumTextField
import com.tattoo.studio.presentation.theme.Dimens
import com.tattoo.studio.presentation.theme.Primary
import androidx.compose.ui.graphics.Color
import com.tattoo.studio.data.remote.AppError
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToVerification: (String) -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        if (error is AppError.Unverified) {
            onNavigateToVerification(email)
            viewModel.clearError()
        }
    }

    MagnumBackground {
        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(padding)
                    .padding(horizontal = Dimens.margin),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                error?.let { err ->
                    if (err !is AppError.Unverified) {
                        MagnumErrorBanner(
                            message = err.userMessage,
                            onDismiss = { viewModel.clearError() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                Text(
                    text = "MAGNUM", 
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, color = Primary)
                )
                Text(
                    text = "STUDIO", 
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 4.sp, color = Primary)
                )
                
                Spacer(modifier = Modifier.height(Dimens.margin * 3))
                
                MagnumTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "EMAIL",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(Dimens.gutter))
                
                MagnumTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "ПАРОЛЬ",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Primary
                            )
                        }
                    }
                )
                
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "ЗАБЫЛИ ПАРОЛЬ?",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
                
                Spacer(modifier = Modifier.height(Dimens.margin * 2))
                
                MagnumButton(
                    text = "ВОЙТИ",
                    onClick = { 
                        scope.launch {
                            if (viewModel.login(email, password)) {
                                onLoginSuccess()
                            }
                        }
                    },
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.height(Dimens.gutter))
                
                MagnumButton(
                    text = "РЕГИСТРАЦИЯ",
                    isSecondary = true,
                    onClick = onNavigateToRegister
                )
            }
        }
    }
}
