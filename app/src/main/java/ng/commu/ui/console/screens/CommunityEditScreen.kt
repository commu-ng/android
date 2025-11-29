package ng.commu.ui.console.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ng.commu.R
import ng.commu.data.model.Community
import ng.commu.data.model.CommunityUpdateRequest
import ng.commu.data.model.CommunityUpdateResponse
import ng.commu.data.model.ImageUploadResponse
import ng.commu.data.repository.CommunityRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class CommunityEditUiState {
    object Form : CommunityEditUiState()
    object Loading : CommunityEditUiState()
    data class Success(val community: CommunityUpdateResponse) : CommunityEditUiState()
    data class Error(val message: String) : CommunityEditUiState()
    object Deleting : CommunityEditUiState()
    object DeleteSuccess : CommunityEditUiState()
}

@HiltViewModel
class CommunityEditViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommunityEditUiState>(CommunityEditUiState.Form)
    val uiState: StateFlow<CommunityEditUiState> = _uiState.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _uploadedImage = MutableStateFlow<ImageUploadResponse?>(null)
    val uploadedImage: StateFlow<ImageUploadResponse?> = _uploadedImage.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    fun uploadImage(tempFile: File) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            _uploadError.value = null

            val result = withContext(Dispatchers.IO) {
                try {
                    val requestBody = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    val filePart = MultipartBody.Part.createFormData(
                        "file",
                        tempFile.name,
                        requestBody
                    )
                    communityRepository.uploadImage(filePart)
                } finally {
                    tempFile.delete()
                }
            }

            result.onSuccess { response ->
                _uploadedImage.value = response
            }.onFailure { error ->
                _uploadError.value = error.message ?: "Failed to upload image"
            }

            _isUploadingImage.value = false
        }
    }

    fun clearImage() {
        _uploadedImage.value = null
        _uploadError.value = null
    }

    fun updateCommunity(
        communityId: String,
        name: String,
        slug: String,
        startDate: Date,
        endDate: Date,
        isRecruiting: Boolean,
        recruitingStartDate: Date?,
        recruitingEndDate: Date?,
        minimumBirthYear: Int?,
        muteNewMembers: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = CommunityEditUiState.Loading

            val request = CommunityUpdateRequest(
                name = name.trim(),
                slug = slug.trim(),
                startsAt = formatDateForApi(startDate),
                endsAt = formatDateForApi(endDate),
                isRecruiting = isRecruiting,
                recruitingStartsAt = recruitingStartDate?.let { formatDateForApi(it) },
                recruitingEndsAt = recruitingEndDate?.let { formatDateForApi(it) },
                minimumBirthYear = minimumBirthYear,
                imageId = _uploadedImage.value?.id,
                hashtags = null,
                description = null,
                muteNewMembers = muteNewMembers
            )

            val result = communityRepository.updateCommunity(communityId, request)
            if (result.isSuccess) {
                _uiState.value = CommunityEditUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = CommunityEditUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update community"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = CommunityEditUiState.Form
    }

    fun deleteCommunity(communityId: String) {
        viewModelScope.launch {
            _uiState.value = CommunityEditUiState.Deleting

            val result = communityRepository.deleteCommunity(communityId)
            if (result.isSuccess) {
                _uiState.value = CommunityEditUiState.DeleteSuccess
            } else {
                _uiState.value = CommunityEditUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to delete community"
                )
            }
        }
    }

    private fun formatDateForApi(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityEditScreen(
    community: Community,
    onNavigateBack: () -> Unit,
    onCommunityUpdated: () -> Unit,
    viewModel: CommunityEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val uploadedImage by viewModel.uploadedImage.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()

    var name by remember { mutableStateOf(community.name) }
    var slug by remember { mutableStateOf(community.slug) }
    var startDate by remember { mutableStateOf(parseDate(community.startsAt) ?: Date()) }
    var endDate by remember { mutableStateOf(parseDate(community.endsAt) ?: Date()) }
    var isRecruiting by remember { mutableStateOf(community.isRecruiting) }
    var recruitingStartDate by remember {
        mutableStateOf(community.recruitingStartsAt?.let { parseDate(it) })
    }
    var recruitingEndDate by remember {
        mutableStateOf(community.recruitingEndsAt?.let { parseDate(it) })
    }
    var minimumBirthYearText by remember {
        mutableStateOf(community.minimumBirthYear?.toString() ?: "")
    }
    var muteNewMembers by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var useExistingBanner by remember { mutableStateOf(community.bannerImageUrl != null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteConfirmSlug by remember { mutableStateOf("") }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            useExistingBanner = false
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
                val outputStream = FileOutputStream(tempFile)

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                viewModel.uploadImage(tempFile)
            } catch (_: Exception) {
            }
        }
    }

    val isFormValid = name.isNotBlank() &&
        slug.isNotBlank() &&
        isValidSlug(slug) &&
        startDate.before(endDate) &&
        !isUploadingImage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community_edit_title)) },
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
            is CommunityEditUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CommunityEditUiState.Success -> {
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
                        text = stringResource(R.string.community_edit_success_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.community_edit_success_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCommunityUpdated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }

            is CommunityEditUiState.Deleting -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.community_delete_deleting))
                    }
                }
            }

            is CommunityEditUiState.DeleteSuccess -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.community_delete_success_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.community_delete_success_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCommunityUpdated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }

            is CommunityEditUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Button(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            is CommunityEditUiState.Form -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Basic Info Section
                    EditFormSection(title = stringResource(R.string.community_create_section_basic)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.community_create_name)) },
                            placeholder = { Text(stringResource(R.string.community_create_name_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = slug,
                            onValueChange = { newValue ->
                                slug = formatSlug(newValue)
                            },
                            label = { Text(stringResource(R.string.community_create_slug)) },
                            placeholder = { Text(stringResource(R.string.community_create_slug_placeholder)) },
                            supportingText = {
                                Column {
                                    Text("${slug.ifEmpty { "example" }}.commu.ng")
                                    if (slug.isNotEmpty() && !isValidSlug(slug)) {
                                        Text(
                                            text = stringResource(R.string.community_create_slug_invalid),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = slug.isNotEmpty() && !isValidSlug(slug)
                        )
                    }

                    // Banner Image Section
                    EditFormSection(title = stringResource(R.string.community_create_section_image)) {
                        if (selectedImageUri != null || uploadedImage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                AsyncImage(
                                    model = uploadedImage?.url ?: selectedImageUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                if (isUploadingImage) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            modifier = Modifier.fillMaxSize()
                                        ) {}
                                        CircularProgressIndicator()
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            selectedImageUri = null
                                            useExistingBanner = false
                                            viewModel.clearImage()
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.community_create_image_remove),
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isUploadingImage) {
                                Text(
                                    text = stringResource(R.string.community_create_image_uploading),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (useExistingBanner && community.bannerImageUrl != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                AsyncImage(
                                    model = community.bannerImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                IconButton(
                                    onClick = {
                                        useExistingBanner = false
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.community_create_image_remove),
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        uploadError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploadingImage
                        ) {
                            Text(
                                if (selectedImageUri == null && uploadedImage == null && !useExistingBanner) {
                                    stringResource(R.string.community_create_image_select)
                                } else {
                                    stringResource(R.string.community_create_image_change)
                                }
                            )
                        }
                    }

                    // Schedule Section
                    EditFormSection(title = stringResource(R.string.community_create_section_schedule)) {
                        EditDateTimeField(
                            label = stringResource(R.string.community_create_start_date),
                            date = startDate,
                            onDateChange = { startDate = it }
                        )

                        EditDateTimeField(
                            label = stringResource(R.string.community_create_end_date),
                            date = endDate,
                            onDateChange = { endDate = it },
                            minDate = startDate
                        )

                        if (!startDate.before(endDate)) {
                            Text(
                                text = stringResource(R.string.community_create_date_invalid),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Recruitment Section
                    EditFormSection(title = stringResource(R.string.community_create_section_recruiting)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.community_create_recruiting))
                            Switch(
                                checked = isRecruiting,
                                onCheckedChange = {
                                    isRecruiting = it
                                    if (it) {
                                        recruitingStartDate = Date()
                                        recruitingEndDate = Calendar.getInstance().apply {
                                            add(Calendar.MONTH, 1)
                                        }.time
                                    } else {
                                        recruitingStartDate = null
                                        recruitingEndDate = null
                                    }
                                }
                            )
                        }
                    }

                    // Recruiting Period Section (shown when recruiting is enabled)
                    if (isRecruiting && recruitingStartDate != null) {
                        EditFormSection(title = stringResource(R.string.community_create_recruiting_period)) {
                            EditDateTimeField(
                                label = stringResource(R.string.community_create_recruiting_start),
                                date = recruitingStartDate!!,
                                onDateChange = { recruitingStartDate = it }
                            )

                            EditDateTimeField(
                                label = stringResource(R.string.community_create_recruiting_end),
                                date = recruitingEndDate ?: Date(),
                                onDateChange = { recruitingEndDate = it },
                                minDate = recruitingStartDate
                            )

                            OutlinedTextField(
                                value = minimumBirthYearText,
                                onValueChange = { newValue ->
                                    minimumBirthYearText = newValue.filter { it.isDigit() }.take(4)
                                },
                                label = { Text(stringResource(R.string.community_create_min_birth_year)) },
                                placeholder = { Text(stringResource(R.string.community_create_min_birth_year_placeholder)) },
                                supportingText = { Text(stringResource(R.string.community_create_min_birth_year_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    // Settings Section
                    EditFormSection(
                        title = stringResource(R.string.community_create_section_settings),
                        description = stringResource(R.string.community_create_mute_new_members_hint)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.community_create_mute_new_members))
                            Switch(
                                checked = muteNewMembers,
                                onCheckedChange = { muteNewMembers = it }
                            )
                        }
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            viewModel.updateCommunity(
                                communityId = community.id,
                                name = name,
                                slug = slug,
                                startDate = startDate,
                                endDate = endDate,
                                isRecruiting = isRecruiting,
                                recruitingStartDate = recruitingStartDate,
                                recruitingEndDate = recruitingEndDate,
                                minimumBirthYear = minimumBirthYearText.toIntOrNull(),
                                muteNewMembers = muteNewMembers
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.community_edit_save))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Danger Zone Section
                    EditFormSection(title = stringResource(R.string.community_delete_section)) {
                        Text(
                            text = stringResource(R.string.community_delete_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.community_delete_button))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmation = false
                    deleteConfirmSlug = ""
                },
                title = { Text(stringResource(R.string.community_delete_confirm_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            stringResource(R.string.community_delete_confirm_message, community.slug)
                        )
                        OutlinedTextField(
                            value = deleteConfirmSlug,
                            onValueChange = { deleteConfirmSlug = it },
                            label = { Text(stringResource(R.string.community_delete_confirm_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCommunity(community.id)
                            showDeleteConfirmation = false
                            deleteConfirmSlug = ""
                        },
                        enabled = deleteConfirmSlug == community.slug,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.community_delete_confirm_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            deleteConfirmSlug = ""
                        }
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun EditFormSection(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        content()
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDateTimeField(
    label: String,
    date: Date,
    onDateChange: (Date) -> Unit,
    minDate: Date? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    OutlinedTextField(
        value = dateFormat.format(date),
        onValueChange = { },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            TextButton(onClick = { showDatePicker = true }) {
                Text(stringResource(R.string.action_change))
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.time,
            selectableDates = if (minDate != null) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= minDate.time - 86400000
                    }
                }
            } else {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = millis
                            val oldCalendar = Calendar.getInstance()
                            oldCalendar.time = date
                            calendar.set(Calendar.HOUR_OF_DAY, oldCalendar.get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, oldCalendar.get(Calendar.MINUTE))
                            onDateChange(calendar.time)
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text(stringResource(R.string.action_next))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCalendar = Calendar.getInstance()
                        newCalendar.time = date
                        newCalendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        newCalendar.set(Calendar.MINUTE, timePickerState.minute)
                        onDateChange(newCalendar.time)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            title = { Text(stringResource(R.string.community_create_select_time)) },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

private fun parseDate(dateString: String): Date? {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(dateString)
    } catch (_: Exception) {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)
        } catch (_: Exception) {
            null
        }
    }
}

private fun formatSlug(input: String): String {
    var result = input
        .lowercase()
        .replace(" ", "-")
        .filter { it.isLetterOrDigit() || it == '-' }

    while (result.contains("--")) {
        result = result.replace("--", "-")
    }

    result = result.trim('-')

    return result
}

private fun isValidSlug(slug: String): Boolean {
    if (slug.isEmpty()) return false
    val pattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    return pattern.matches(slug)
}
