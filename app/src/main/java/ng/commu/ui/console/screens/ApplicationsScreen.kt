package ng.commu.ui.console.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import ng.commu.data.model.CommunityApplicationDetail
import ng.commu.data.repository.CommunityRepository
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

sealed class ApplicationsUiState {
    object Loading : ApplicationsUiState()
    data class Success(val applications: List<CommunityApplicationDetail>) : ApplicationsUiState()
    data class Error(val message: String) : ApplicationsUiState()
}

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ApplicationsUiState>(ApplicationsUiState.Loading)
    val uiState: StateFlow<ApplicationsUiState> = _uiState.asStateFlow()

    private val _isApproving = MutableStateFlow(false)
    val isApproving: StateFlow<Boolean> = _isApproving.asStateFlow()

    private val _isRejecting = MutableStateFlow(false)
    val isRejecting: StateFlow<Boolean> = _isRejecting.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun loadApplications(slug: String) {
        viewModelScope.launch {
            _uiState.value = ApplicationsUiState.Loading
            val result = communityRepository.getCommunityApplications(slug)
            _uiState.value = if (result.isSuccess) {
                ApplicationsUiState.Success(result.getOrDefault(emptyList()))
            } else {
                ApplicationsUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun approveApplication(slug: String, applicationId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isApproving.value = true
            _actionError.value = null
            val result = communityRepository.approveApplication(slug, applicationId)
            _isApproving.value = false
            if (result.isSuccess) {
                onSuccess()
                loadApplications(slug)
            } else {
                _actionError.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun rejectApplication(slug: String, applicationId: String, reason: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isRejecting.value = true
            _actionError.value = null
            val result = communityRepository.rejectApplication(slug, applicationId, reason)
            _isRejecting.value = false
            if (result.isSuccess) {
                onSuccess()
                loadApplications(slug)
            } else {
                _actionError.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    communitySlug: String,
    onNavigateBack: () -> Unit,
    viewModel: ApplicationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedApplication by remember { mutableStateOf<CommunityApplicationDetail?>(null) }

    LaunchedEffect(communitySlug) {
        viewModel.loadApplications(communitySlug)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.applications_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ApplicationsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ApplicationsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadApplications(communitySlug) }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            is ApplicationsUiState.Success -> {
                if (state.applications.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.applications_empty),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.applications_empty_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val pendingApplications = state.applications.filter { it.status == "pending" }
                    val reviewedApplications = state.applications.filter { it.status != "pending" }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pending Section
                        if (pendingApplications.isNotEmpty()) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.applications_pending),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "(${pendingApplications.size})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(pendingApplications, key = { "pending-${it.id}" }) { application ->
                                ApplicationRow(
                                    application = application,
                                    onClick = { selectedApplication = application }
                                )
                            }
                        }

                        // Reviewed Section
                        if (reviewedApplications.isNotEmpty()) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = stringResource(R.string.applications_reviewed),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "(${reviewedApplications.size})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(reviewedApplications, key = { "reviewed-${it.id}" }) { application ->
                                ApplicationRow(
                                    application = application,
                                    onClick = { selectedApplication = application }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Application Detail Bottom Sheet
    selectedApplication?.let { application ->
        ApplicationDetailSheet(
            communitySlug = communitySlug,
            application = application,
            viewModel = viewModel,
            onDismiss = { selectedApplication = null }
        )
    }
}

@Composable
private fun ApplicationRow(
    application: CommunityApplicationDetail,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = application.profileName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = application.profileName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    ApplicationStatusBadge(status = application.status)
                }

                Text(
                    text = "@${application.profileUsername}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
private fun ApplicationStatusBadge(status: String) {
    val (text, backgroundColor, textColor) = when (status) {
        "pending" -> Triple(
            stringResource(R.string.application_status_pending),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        "approved" -> Triple(
            stringResource(R.string.application_status_approved),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "rejected" -> Triple(
            stringResource(R.string.application_status_rejected),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        else -> Triple(
            status,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationDetailSheet(
    communitySlug: String,
    application: CommunityApplicationDetail,
    viewModel: ApplicationsViewModel,
    onDismiss: () -> Unit
) {
    val isApproving by viewModel.isApproving.collectAsState()
    val isRejecting by viewModel.isRejecting.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var showRejectDialog by remember { mutableStateOf(false) }

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

                        ApplicationStatusBadge(status = application.status)
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

                    // Reviewed on (if reviewed)
                    application.reviewedAt?.let { reviewedAt ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.applications_reviewed_on),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = formatDate(reviewedAt),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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

            // Error message
            actionError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Action Buttons (only for pending)
            if (application.status == "pending") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.approveApplication(communitySlug, application.id) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isApproving && !isRejecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isApproving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (isApproving) stringResource(R.string.applications_approving)
                            else stringResource(R.string.applications_approve)
                        )
                    }

                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isApproving && !isRejecting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.applications_reject))
                    }
                }
            }
        }
    }

    // Reject Dialog
    if (showRejectDialog) {
        var rejectionReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text(stringResource(R.string.applications_reject_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.applications_reject_message))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.applications_rejection_reason_placeholder)) },
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRejectDialog = false
                        viewModel.rejectApplication(
                            communitySlug,
                            application.id,
                            rejectionReason.takeIf { it.isNotBlank() }
                        ) {
                            onDismiss()
                        }
                    },
                    enabled = !isRejecting
                ) {
                    Text(stringResource(R.string.applications_reject))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val zonedDateTime = ZonedDateTime.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
        zonedDateTime.format(formatter)
    } catch (e: DateTimeParseException) {
        dateString
    }
}
