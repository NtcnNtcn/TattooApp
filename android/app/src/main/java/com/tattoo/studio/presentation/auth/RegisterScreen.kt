package com.tattoo.studio.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.VisualTransformation
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
import com.tattoo.studio.presentation.theme.Outline
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("client") } // client or master
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

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
                    MagnumErrorBanner(
                        message = err.userMessage,
                        onDismiss = { viewModel.clearError() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = "СОЗДАТЬ", 
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, color = Primary)
                )
                Text(
                    text = "АККАУНТ", 
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 4.sp, color = Primary)
                )
                
                Spacer(modifier = Modifier.height(Dimens.margin * 2))
                
                MagnumTextField(
                    value = fullName,
                    onValueChange = { 
                        fullName = it 
                        nameError = if (it.length < 2 && it.isNotEmpty()) "Минимум 2 символа" else null
                    },
                    label = "ИМЯ ФАМИЛИЯ",
                    isError = nameError != null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    )
                )
                if (nameError != null) {
                    Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(Dimens.gutter))
                
                MagnumTextField(
                    value = email,
                    onValueChange = { 
                        email = it 
                        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
                        emailError = if (it.isNotEmpty() && !it.matches(emailRegex)) "Неверный формат почты" else null
                    },
                    label = "EMAIL",
                    isError = emailError != null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    )
                )
                if (emailError != null) {
                    Text(emailError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(Dimens.gutter))
                
                MagnumTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        // Simple inline validation feedback
                        passwordError = when {
                            it.isEmpty() -> null
                            it.length < 8 -> "Минимум 8 символов"
                            !it.any { c -> c.isUpperCase() } -> "Нужна заглавная буква"
                            !it.any { c -> c.isLowerCase() } -> "Нужна строчная буква"
                            !it.any { c -> "!@#$%^&*(),.?\":{}|<>".contains(c) } -> "Нужен спецсимвол"
                            else -> null
                        }
                    },
                    label = "ПАРОЛЬ",
                    isError = passwordError != null,
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
                if (passwordError != null) {
                    Text(passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
                } else {
                    Text("8+ симв, A-Z, a-z, #$%", color = Outline, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(Dimens.gutter))
                
                // Role selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = role == "client",
                            onClick = { role = "client" },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = Primary.copy(alpha = 0.5f))
                        )
                        Text("КЛИЕНТ", style = MaterialTheme.typography.labelSmall.copy(color = Primary))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = role == "master",
                            onClick = { role = "master" },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = Primary.copy(alpha = 0.5f))
                        )
                        Text("МАСТЕР", style = MaterialTheme.typography.labelSmall.copy(color = Primary))
                    }
                }
                
                Spacer(modifier = Modifier.height(Dimens.margin))
                
                MagnumButton(
                    text = "ЗАРЕГИСТРИРОВАТЬСЯ",
                    onClick = { 
                        // Final validation check before send
                        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
                        val isEmailValid = email.matches(emailRegex)
                        val isNameValid = fullName.length in 2..255
                        val isPasswordValid = password.length >= 8 && 
                            password.any { it.isUpperCase() } && 
                            password.any { it.isLowerCase() } && 
                            password.any { "!@#$%^&*(),.?\":{}|<>".contains(it) }
                        
                        if (isNameValid && isPasswordValid && isEmailValid) {
                            scope.launch {
                                if (viewModel.register(email, password, fullName, role)) {
                                    onRegisterSuccess(email)
                                }
                            }
                        } else {
                            if (!isEmailValid) emailError = "Проверьте почту"
                            if (!isNameValid) nameError = "Проверьте имя"
                            if (!isPasswordValid) passwordError = "Слабый пароль"
                        }
                    },
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.height(Dimens.gutter))
                
                MagnumButton(
                    text = "НАЗАД К ВХОДУ",
                    isSecondary = true,
                    onClick = onNavigateToLogin
                )
            }
        }
    }
}
