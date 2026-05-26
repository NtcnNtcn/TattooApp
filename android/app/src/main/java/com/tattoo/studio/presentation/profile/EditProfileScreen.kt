package com.tattoo.studio.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.tattoo.studio.presentation.components.MagnumErrorState
import com.tattoo.studio.presentation.components.MagnumTextField
import com.tattoo.studio.presentation.components.MagnumTopAppBar
import com.tattoo.studio.presentation.theme.Dimens
import com.tattoo.studio.presentation.theme.Outline
import com.tattoo.studio.presentation.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onNavigateToVerification: (String) -> Unit,
    onDeleteSuccess: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentProfile()
    }

    MagnumBackground {
        Scaffold(
            topBar = {
                MagnumTopAppBar(
                    onNavigationClick = onBack
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is EditProfileUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Primary
                        )
                    }
                    is EditProfileUiState.Error -> {
                        MagnumErrorState(error = state.error)
                    }
                    is EditProfileUiState.Success -> {
                        EditProfileContent(
                            initialFullName = state.fullName,
                            initialEmail = state.email,
                            initialDescription = state.description,
                            error = state.error,
                            isSaving = isSaving,
                            onSave = { fullName, email, description ->
                                viewModel.saveProfile(fullName, email, description, onBack, onNavigateToVerification)
                            },
                            onDeleteAccount = {
                                viewModel.deleteAccount(onDeleteSuccess)
                            }
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun EditProfileContent(
    initialFullName: String,
    initialEmail: String,
    initialDescription: String,
    error: String?,
    isSaving: Boolean,
    onSave: (String, String, String) -> Unit,
    onDeleteAccount: () -> Unit
) {
    var fullName by remember { mutableStateOf(initialFullName) }
    var email by remember { mutableStateOf(initialEmail) }
    var description by remember { mutableStateOf(initialDescription) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    val hasChanges = fullName != initialFullName ||
            email != initialEmail ||
            description != initialDescription
    val emailChanged = email != initialEmail

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteConfirmText = ""
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    "УДАЛИТЬ АККАУНТ?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Это действие необратимо. Все ваши данные, работы и история будут удалены навсегда.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Для подтверждения введите УДАЛИТЬ:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Outline)
                    )
                    MagnumTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        label = "УДАЛИТЬ",
                        modifier = Modifier.fillMaxWidth(),
                        isError = deleteConfirmText.isNotEmpty() && deleteConfirmText != "УДАЛИТЬ"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteConfirmText = ""
                        onDeleteAccount()
                    },
                    enabled = deleteConfirmText == "УДАЛИТЬ"
                ) {
                    Text(
                        "УДАЛИТЬ",
                        color = if (deleteConfirmText == "УДАЛИТЬ")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deleteConfirmText = ""
                }) {
                    Text("ОТМЕНА")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.margin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "РЕДАКТИРОВАНИЕ ПРОФИЛЯ",
            style = MaterialTheme.typography.headlineSmall.copy(color = Primary),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        MagnumTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "ПОЛНОЕ ИМЯ",
            modifier = Modifier.fillMaxWidth()
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            MagnumTextField(
                value = email,
                onValueChange = { email = it },
                label = "EMAIL",
                modifier = Modifier.fillMaxWidth()
            )
            if (emailChanged) {
                Text(
                    text = "Потребуется повторная верификация",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            MagnumTextField(
                value = description,
                onValueChange = { if (it.length <= 300) description = it },
                label = "О СЕБЕ",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Расскажите о своём опыте и стиле",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
                Text(
                    text = "${description.length}/300",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (description.length >= 280) MaterialTheme.colorScheme.error else Color.Gray
                    )
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onSave(fullName, email, description) },
            enabled = !isSaving && hasChanges && fullName.isNotBlank() && email.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = MaterialTheme.colorScheme.background,
                disabledContainerColor = Primary.copy(alpha = 0.4f),
                disabledContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4f)
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.background,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (hasChanges) "СОХРАНИТЬ" else "НЕТ ИЗМЕНЕНИЙ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
            shape = MaterialTheme.shapes.extraSmall,
            enabled = !isSaving
        ) {
            Text(
                "УДАЛИТЬ АККАУНТ",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
