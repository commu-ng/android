package ng.commu.ui.console.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ng.commu.R
import ng.commu.viewmodel.AccountSettingsViewModel
import ng.commu.viewmodel.ChangeEmailViewModel
import ng.commu.viewmodel.ChangePasswordViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit = {},
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            // Error displayed in UI, clear after showing
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    // Show success snackbar
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
            ) {
                if (isLoading && currentUser == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    currentUser?.let { user ->
                        // Account Information
                        Text(
                            text = stringResource(R.string.account_information),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                        )

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                InfoRow(stringResource(R.string.account_login_name), user.loginName)

                                user.email?.let { email ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column {
                                        InfoRow(stringResource(R.string.account_email), email)
                                        if (user.emailVerifiedAt != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = stringResource(R.string.account_verified),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    stringResource(R.string.account_verified),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                InfoRow(stringResource(R.string.account_joined), formatDate(user.createdAt))
                            }
                        }

                        // Security
                        Text(
                            text = stringResource(R.string.account_security),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
                        )

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.account_change_password)) },
                                    leadingContent = {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                    },
                                    trailingContent = {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                                Modifier
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { showChangePasswordDialog = true }
                                )

                                if (user.email != null) {
                                    HorizontalDivider()
                                    ListItem(
                                        headlineContent = { Text(stringResource(R.string.account_change_email)) },
                                        leadingContent = {
                                            Icon(Icons.Default.Email, contentDescription = null)
                                        },
                                        trailingContent = {
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showChangeEmailDialog = true }
                                    )
                                }
                            }
                        }

                        // Blocked Users
                        Text(
                            text = stringResource(R.string.block_section),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
                        )

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.block_manage)) },
                                leadingContent = {
                                    Icon(Icons.Default.PersonOff, contentDescription = null)
                                },
                                trailingContent = {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToBlockedUsers() }
                            )
                        }

                        // Logout & Delete
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.auth_logout))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showDeleteAccountDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.account_delete_account))
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // Error/Success messages
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(error)
                }
            }

            successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(message)
                }
            }
        }
    }

    // Dialogs
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSuccess = { showChangePasswordDialog = false }
        )
    }

    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            onDismiss = { showChangeEmailDialog = false },
            onSuccess = { showChangeEmailDialog = false }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            isDeleting = isDeleting,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = {
                viewModel.deleteAccount {
                    showDeleteAccountDialog = false
                    onLogout()
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val isValid = currentPassword.isNotBlank() &&
            newPassword.isNotBlank() &&
            newPassword.length >= 8 &&
            newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_change_password)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.account_current_password)) },
                    visualTransformation = if (showCurrentPassword)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                            Icon(
                                if (showCurrentPassword) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.account_new_password)) },
                    visualTransformation = if (showNewPassword)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    supportingText = { Text(stringResource(R.string.account_password_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.account_confirm_password)) },
                    visualTransformation = if (showConfirmPassword)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                if (showConfirmPassword) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    isError = confirmPassword.isNotBlank() && newPassword != confirmPassword,
                    supportingText = {
                        if (confirmPassword.isNotBlank() && newPassword != confirmPassword) {
                            Text(stringResource(R.string.account_passwords_mismatch))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.changePassword(currentPassword, newPassword, onSuccess)
                },
                enabled = isValid && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.account_change_password))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ChangeEmailDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ChangeEmailViewModel = hiltViewModel()
) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    val isValid = newEmail.isNotBlank() &&
        android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches() &&
        password.isNotBlank()

    // Auto-close on success
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(2000)
            onSuccess()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_change_email)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (successMessage != null) {
                    Text(
                        text = successMessage!!,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text(stringResource(R.string.account_new_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.account_current_password)) },
                    visualTransformation = if (showPassword)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.account_verification_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.changeEmail(newEmail, password) },
                enabled = isValid && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.account_change_email))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.account_delete_confirm_title)) },
        text = {
            Text(
                stringResource(R.string.account_delete_warning)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text(stringResource(R.string.action_delete))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun formatDate(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
        val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val date = inputFormat.parse(isoString)
        date?.let { outputFormat.format(it) } ?: isoString
    } catch (e: Exception) {
        isoString
    }
}
