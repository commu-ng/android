package ng.commu.ui.console.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.R
import ng.commu.data.model.CommunityApplication
import ng.commu.data.repository.CommunityRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class ApplicationUiState {
    object Loading : ApplicationUiState()
    data class Form(
        val existingApplications: List<CommunityApplication>,
        val hasPendingApplication: Boolean
    ) : ApplicationUiState()
    object Success : ApplicationUiState()
    data class Error(val message: String) : ApplicationUiState()
}

@HiltViewModel
class CommunityApplicationViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ApplicationUiState>(ApplicationUiState.Loading)
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    fun loadExistingApplications(slug: String) {
        viewModelScope.launch {
            _uiState.value = ApplicationUiState.Loading

            val result = communityRepository.getMyApplications(slug)
            if (result.isSuccess) {
                val applications = result.getOrThrow()
                val hasPending = applications.any { it.status == "pending" }
                _uiState.value = ApplicationUiState.Form(
                    existingApplications = applications,
                    hasPendingApplication = hasPending
                )
            } else {
                // If we can't load applications, still show the form
                _uiState.value = ApplicationUiState.Form(
                    existingApplications = emptyList(),
                    hasPendingApplication = false
                )
            }
        }
    }

    fun submitApplication(
        slug: String,
        profileName: String,
        profileUsername: String,
        message: String?
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _submitError.value = null

            val result = communityRepository.applyToCommunity(
                slug = slug,
                profileName = profileName.trim(),
                profileUsername = profileUsername.trim(),
                message = if (message.isNullOrBlank()) null else message.trim()
            )

            if (result.isSuccess) {
                _uiState.value = ApplicationUiState.Success
            } else {
                _submitError.value = result.exceptionOrNull()?.message ?: "Failed to submit application"
            }

            _isSubmitting.value = false
        }
    }

    fun clearError() {
        _submitError.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityApplicationScreen(
    slug: String,
    communityName: String,
    onNavigateBack: () -> Unit,
    viewModel: CommunityApplicationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val submitError by viewModel.submitError.collectAsState()

    var profileName by remember { mutableStateOf("") }
    var profileUsername by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedApplication by remember { mutableStateOf<CommunityApplication?>(null) }

    LaunchedEffect(slug) {
        viewModel.loadExistingApplications(slug)
    }

    // My Application Detail Sheet
    selectedApplication?.let { application ->
        MyApplicationDetailSheet(
            application = application,
            onDismiss = { selectedApplication = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.application_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ApplicationUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ApplicationUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.application_success_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.application_success_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }

            is ApplicationUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadExistingApplications(slug) }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            is ApplicationUiState.Form -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Community Header
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = communityName,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "@$slug",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Existing Applications
                    if (state.existingApplications.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.application_my_applications),
                            style = MaterialTheme.typography.titleMedium
                        )

                        state.existingApplications.forEach { application ->
                            ApplicationCard(
                                application = application,
                                onClick = { selectedApplication = application }
                            )
                        }
                    }

                    // Show form only if no pending application
                    if (!state.hasPendingApplication) {
                        if (state.existingApplications.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.application_reapply),
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.application_form_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = stringResource(R.string.application_form_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Profile Name
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text(stringResource(R.string.application_profile_name)) },
                            placeholder = { Text(stringResource(R.string.application_profile_name_placeholder)) },
                            supportingText = { Text(stringResource(R.string.application_profile_name_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting,
                            singleLine = true
                        )

                        // Profile Username
                        OutlinedTextField(
                            value = profileUsername,
                            onValueChange = { newValue ->
                                // Only allow alphanumeric and underscore
                                profileUsername = newValue
                                    .replace(" ", "_")
                                    .filter { it.isLetterOrDigit() || it == '_' }
                            },
                            label = { Text(stringResource(R.string.application_profile_username)) },
                            placeholder = { Text(stringResource(R.string.application_profile_username_placeholder)) },
                            supportingText = {
                                Text("@${profileUsername.ifEmpty { "username" }}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                        )

                        // Message (Optional)
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text(stringResource(R.string.application_message)) },
                            placeholder = { Text(stringResource(R.string.application_message_placeholder)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            enabled = !isSubmitting,
                            maxLines = 5
                        )

                        // Error Message
                        if (submitError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = submitError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                viewModel.clearError()
                                viewModel.submitApplication(
                                    slug = slug,
                                    profileName = profileName,
                                    profileUsername = profileUsername,
                                    message = message.ifBlank { null }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting && profileName.isNotBlank() && profileUsername.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.application_submitting))
                            } else {
                                Text(stringResource(R.string.application_submit))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    application: CommunityApplication,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${application.profileName} (@${application.profileUsername})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    StatusBadge(status = application.status)
                }

                application.rejectionReason?.let { reason ->
                    Text(
                        text = stringResource(R.string.application_rejection_reason, reason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }

                Text(
                    text = formatDate(application.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, containerColor, contentColor) = when (status) {
        "pending" -> Triple(
            stringResource(R.string.application_status_pending),
            Color(0xFFFFF3CD),
            Color(0xFF856404)
        )
        "approved" -> Triple(
            stringResource(R.string.application_status_approved),
            Color(0xFFD4EDDA),
            Color(0xFF155724)
        )
        "rejected" -> Triple(
            stringResource(R.string.application_status_rejected),
            Color(0xFFF8D7DA),
            Color(0xFF721C24)
        )
        else -> Triple(status, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyApplicationDetailSheet(
    application: CommunityApplication,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.applications_detail_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                }
            }

            // Applicant Info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = application.profileName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = application.profileName,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "@${application.profileUsername}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        StatusBadge(status = application.status)
                    }

                    HorizontalDivider()

                    // Applied on
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.applications_applied_on),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = formatDate(application.createdAt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Message
            application.message?.takeIf { it.isNotBlank() }?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.applications_message),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Rejection Reason
            if (application.status == "rejected") {
                application.rejectionReason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.applications_rejection_reason_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        outputFormat.format(date ?: return dateString)
    } catch (e: Exception) {
        dateString
    }
}
