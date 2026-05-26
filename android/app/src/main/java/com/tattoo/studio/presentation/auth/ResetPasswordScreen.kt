package com.tattoo.studio.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tattoo.studio.presentation.components.MagnumBackground
import com.tattoo.studio.presentation.components.MagnumButton
import com.tattoo.studio.presentation.components.MagnumTextField
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.theme.Dimens
import com.tattoo.studio.presentation.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    email: String,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    MagnumBackground {
        Scaffold(
            topBar = { MagnumTopAppBar(onNavigationClick = onBack) },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Dimens.margin),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "СБРОС ПАРОЛЯ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Primary,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                MagnumTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = "КОД ИЗ ПИСЬМА",
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                MagnumTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "НОВЫЙ ПАРОЛЬ",
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                MagnumButton(
                    text = if (isLoading) "СБРОС..." else "УСТАНОВИТЬ ПАРОЛЬ",
                    onClick = { viewModel.resetPassword(email, code, newPassword, onSuccess) },
                    enabled = code.length == 6 && newPassword.length >= 8 && !isLoading
                )
            }
        }
    }
}
