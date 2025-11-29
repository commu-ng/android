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
import ng.commu.data.model.CommunityCreateRequest
import ng.commu.data.model.CommunityCreateResponse
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

sealed class CommunityCreationUiState {
    object Form : CommunityCreationUiState()
    object Loading : CommunityCreationUiState()
    data class Success(val community: CommunityCreateResponse) : CommunityCreationUiState()
    data class Error(val message: String) : CommunityCreationUiState()
}

@HiltViewModel
class CommunityCreationViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommunityCreationUiState>(CommunityCreationUiState.Form)
    val uiState: StateFlow<CommunityCreationUiState> = _uiState.asStateFlow()

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
                    // Clean up temp file after upload completes
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

    fun createCommunity(
        name: String,
        slug: String,
        profileName: String,
        profileUsername: String,
        startDate: Date,
        endDate: Date,
        isRecruiting: Boolean,
        recruitingStartDate: Date?,
        recruitingEndDate: Date?,
        minimumBirthYear: Int?,
        muteNewMembers: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = CommunityCreationUiState.Loading

            val request = CommunityCreateRequest(
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
                profileUsername = profileUsername.trim(),
                profileName = profileName.trim(),
                description = null,
                muteNewMembers = muteNewMembers
            )

            val result = communityRepository.createCommunity(request)
            if (result.isSuccess) {
                _uiState.value = CommunityCreationUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = CommunityCreationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to create community"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = CommunityCreationUiState.Form
    }

    private fun formatDateForApi(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityCreationScreen(
    onNavigateBack: () -> Unit,
    onCommunityCreated: () -> Unit,
    viewModel: CommunityCreationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val uploadedImage by viewModel.uploadedImage.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()

    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }
    var profileUsername by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply { add(Calendar.MONTH, 1) }.time
        )
    }
    var isRecruiting by remember { mutableStateOf(false) }
    var recruitingStartDate by remember { mutableStateOf<Date?>(null) }
    var recruitingEndDate by remember { mutableStateOf<Date?>(null) }
    var minimumBirthYearText by remember { mutableStateOf("") }
    var muteNewMembers by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Upload the image
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
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    val isFormValid = name.isNotBlank() &&
        slug.isNotBlank() &&
        isValidSlug(slug) &&
        profileName.isNotBlank() &&
        profileUsername.isNotBlank() &&
        startDate.before(endDate) &&
        !isUploadingImage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community_create_title)) },
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
            is CommunityCreationUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CommunityCreationUiState.Success -> {
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
                        text = stringResource(R.string.community_create_success_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.community.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${state.community.domain}.commu.ng",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.community_create_success_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCommunityCreated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }

            is CommunityCreationUiState.Error -> {
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

            is CommunityCreationUiState.Form -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Basic Info Section
                    FormSection(title = stringResource(R.string.community_create_section_basic)) {
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
                    FormSection(title = stringResource(R.string.community_create_section_image)) {
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
                                if (selectedImageUri == null && uploadedImage == null) {
                                    stringResource(R.string.community_create_image_select)
                                } else {
                                    stringResource(R.string.community_create_image_change)
                                }
                            )
                        }
                    }

                    // Owner Profile Section
                    FormSection(
                        title = stringResource(R.string.community_create_section_profile),
                        description = stringResource(R.string.community_create_section_profile_footer)
                    ) {
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text(stringResource(R.string.community_create_profile_name)) },
                            placeholder = { Text(stringResource(R.string.community_create_profile_name_placeholder)) },
                            supportingText = { Text(stringResource(R.string.community_create_profile_name_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = profileUsername,
                            onValueChange = { newValue ->
                                profileUsername = newValue
                                    .replace(" ", "_")
                                    .filter { it.isLetterOrDigit() || it == '_' }
                            },
                            label = { Text(stringResource(R.string.community_create_profile_username)) },
                            placeholder = { Text(stringResource(R.string.community_create_profile_username_placeholder)) },
                            supportingText = {
                                Text("@${profileUsername.ifEmpty { "username" }}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                        )
                    }

                    // Schedule Section
                    FormSection(title = stringResource(R.string.community_create_section_schedule)) {
                        DateTimeField(
                            label = stringResource(R.string.community_create_start_date),
                            date = startDate,
                            onDateChange = { startDate = it }
                        )

                        DateTimeField(
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
                    FormSection(title = stringResource(R.string.community_create_section_recruiting)) {
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
                                        // Initialize recruiting dates when enabling recruiting
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
                        FormSection(title = stringResource(R.string.community_create_recruiting_period)) {
                            DateTimeField(
                                label = stringResource(R.string.community_create_recruiting_start),
                                date = recruitingStartDate!!,
                                onDateChange = { recruitingStartDate = it }
                            )

                            DateTimeField(
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
                    FormSection(
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
                            viewModel.createCommunity(
                                name = name,
                                slug = slug,
                                profileName = profileName,
                                profileUsername = profileUsername,
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
                        Text(stringResource(R.string.community_create_submit))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FormSection(
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
private fun DateTimeField(
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
                        return utcTimeMillis >= minDate.time - 86400000 // Allow same day
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

private fun formatSlug(input: String): String {
    var result = input
        .lowercase()
        .replace(" ", "-")
        .filter { it.isLetterOrDigit() || it == '-' }

    // Remove consecutive hyphens
    while (result.contains("--")) {
        result = result.replace("--", "-")
    }

    // Remove leading/trailing hyphens
    result = result.trim('-')

    return result
}

private fun isValidSlug(slug: String): Boolean {
    if (slug.isEmpty()) return false
    val pattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    return pattern.matches(slug)
}
