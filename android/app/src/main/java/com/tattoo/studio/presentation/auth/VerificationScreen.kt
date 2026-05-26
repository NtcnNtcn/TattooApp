package com.tattoo.studio.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun VerificationScreen(
    email: String,
    type: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    var code by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val resendTimer by viewModel.resendTimer.collectAsState()
    val awaitingApproval by viewModel.awaitingApproval.collectAsState()

    if (awaitingApproval) {
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
                        text = "ОЖИДАЙТЕ ОДОБРЕНИЯ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Primary,
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Ваш аккаунт мастера подтверждён. После одобрения администратором \nвы сможете войти в аппликацию.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    MagnumButton(
                        text = "НА ГЛАВНУЮ",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        return
    }

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
                    text = "ПОДТВЕРЖДЕНИЕ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Primary,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Мы отправили код подтверждения на почту\n$email",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                MagnumTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = "КОД ИЗ ПИСЬМА",
                    modifier = Modifier.width(200.dp),
                    singleLine = true
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
                    text = if (isLoading) "ПРОВЕРКА..." else "ПОДТВЕРДИТЬ",
                    onClick = { viewModel.verifyCode(email, code, type, onVerified) },
                    enabled = code.length == 6 && !isLoading
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = { viewModel.resendCode(email, type) },
                    enabled = resendTimer == 0
                ) {
                    Text(
                        text = if (resendTimer > 0) "ОТПРАВИТЬ ПОВТОРНО ЧЕРЕЗ $resendTimer" else "ОТПРАВИТЬ КОД ПОВТОРНО",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (resendTimer > 0) Color.Gray else Primary,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}
