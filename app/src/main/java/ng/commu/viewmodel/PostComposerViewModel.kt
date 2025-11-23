package ng.commu.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.CommunityPost
import ng.commu.data.model.Profile
import ng.commu.data.repository.PostRepository
import ng.commu.data.repository.ProfileRepository
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class PostComposerViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PostComposerViewModel"
        const val MAX_IMAGES = 4
    }

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _uploadedImageIds = MutableStateFlow<List<String>>(emptyList())
    val uploadedImageIds: StateFlow<List<String>> = _uploadedImageIds.asStateFlow()

    private val _isAnnouncement = MutableStateFlow(false)
    val isAnnouncement: StateFlow<Boolean> = _isAnnouncement.asStateFlow()

    private val _contentWarning = MutableStateFlow<String?>(null)
    val contentWarning: StateFlow<String?> = _contentWarning.asStateFlow()

    private val _isScheduled = MutableStateFlow(false)
    val isScheduled: StateFlow<Boolean> = _isScheduled.asStateFlow()

    private val _scheduledDateTime = MutableStateFlow<ZonedDateTime?>(null)
    val scheduledDateTime: StateFlow<ZonedDateTime?> = _scheduledDateTime.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createdPost = MutableStateFlow<CommunityPost?>(null)
    val createdPost: StateFlow<CommunityPost?> = _createdPost.asStateFlow()

    // Mention system
    private val _allProfiles = MutableStateFlow<List<Profile>>(emptyList())
    val allProfiles: StateFlow<List<Profile>> = _allProfiles.asStateFlow()

    private val _mentionProfiles = MutableStateFlow<List<Profile>>(emptyList())
    val mentionProfiles: StateFlow<List<Profile>> = _mentionProfiles.asStateFlow()

    private val _showMentionDropdown = MutableStateFlow(false)
    val showMentionDropdown: StateFlow<Boolean> = _showMentionDropdown.asStateFlow()

    private val _mentionQuery = MutableStateFlow("")
    val mentionQuery: StateFlow<String> = _mentionQuery.asStateFlow()

    private var mentionStartIndex: Int = -1

    fun updateContent(text: String) {
        _content.value = text
        checkForMention(text)
    }

    fun loadProfiles() {
        viewModelScope.launch {
            try {
                val response = profileRepository.getCommunityProfiles()
                if (response.isSuccessful) {
                    _allProfiles.value = response.body()?.data ?: emptyList()
                    Log.d(TAG, "Loaded ${_allProfiles.value.size} profiles for mentions")
                } else {
                    Log.e(TAG, "Failed to load profiles: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profiles", e)
            }
        }
    }

    private fun checkForMention(text: String) {
        // Find the last @ symbol
        val lastAtIndex = text.lastIndexOf('@')

        if (lastAtIndex == -1) {
            _showMentionDropdown.value = false
            _mentionQuery.value = ""
            return
        }

        val afterAt = text.substring(lastAtIndex + 1)

        // Check if there's a space or newline after @ (completed or not a mention)
        if (afterAt.contains(' ') || afterAt.contains('\n')) {
            _showMentionDropdown.value = false
            _mentionQuery.value = ""
            return
        }

        // Validate mention pattern (alphanumeric and underscore)
        val validPattern = Regex("^[a-zA-Z0-9_]*$")
        if (!validPattern.matches(afterAt)) {
            _showMentionDropdown.value = false
            _mentionQuery.value = ""
            return
        }

        mentionStartIndex = lastAtIndex
        _mentionQuery.value = afterAt
        _showMentionDropdown.value = true

        // Filter profiles
        val query = afterAt.lowercase()
        _mentionProfiles.value = if (query.isEmpty()) {
            _allProfiles.value.take(10)
        } else {
            _allProfiles.value.filter { profile ->
                profile.name.lowercase().contains(query) ||
                profile.username.lowercase().contains(query)
            }.take(10)
        }
    }

    fun selectMention(profile: Profile) {
        if (mentionStartIndex == -1) return

        val currentContent = _content.value
        val beforeMention = currentContent.substring(0, mentionStartIndex)
        val afterMention = currentContent.substring(mentionStartIndex + 1 + _mentionQuery.value.length)

        _content.value = "$beforeMention@${profile.username} $afterMention"
        _showMentionDropdown.value = false
        _mentionQuery.value = ""
        _mentionProfiles.value = emptyList()
        mentionStartIndex = -1
    }

    fun dismissMentionDropdown() {
        _showMentionDropdown.value = false
        _mentionProfiles.value = emptyList()
    }

    fun addImage(uri: Uri) {
        if (_selectedImages.value.size < MAX_IMAGES) {
            _selectedImages.value = _selectedImages.value + uri
        }
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value.filter { it != uri }
    }

    fun toggleAnnouncement() {
        _isAnnouncement.value = !_isAnnouncement.value
    }

    fun setContentWarning(warning: String?) {
        _contentWarning.value = warning
    }

    fun toggleScheduling() {
        _isScheduled.value = !_isScheduled.value
        if (_isScheduled.value && _scheduledDateTime.value == null) {
            // Default to 1 hour from now
            _scheduledDateTime.value = ZonedDateTime.now().plusHours(1)
        }
    }

    fun setScheduledDateTime(dateTime: ZonedDateTime) {
        _scheduledDateTime.value = dateTime
    }

    suspend fun uploadImages(imageFiles: List<File>): Boolean {
        _isUploading.value = true
        _errorMessage.value = null
        val uploadedIds = mutableListOf<String>()

        try {
            for (file in imageFiles) {
                val response = postRepository.uploadImage(file)
                if (response.isSuccessful) {
                    val imageId = response.body()?.id
                    if (imageId != null) {
                        uploadedIds.add(imageId)
                        Log.d(TAG, "Uploaded image: $imageId")
                    }
                } else {
                    _errorMessage.value = "Failed to upload image: ${response.code()}"
                    Log.e(TAG, "Failed to upload image: ${response.errorBody()?.string()}")
                    return false
                }
            }

            _uploadedImageIds.value = uploadedIds
            return true
        } catch (e: Exception) {
            _errorMessage.value = "Error uploading images: ${e.localizedMessage}"
            Log.e(TAG, "Failed to upload images", e)
            return false
        } finally {
            _isUploading.value = false
        }
    }

    fun createPost(
        profileId: String,
        inReplyToId: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (_content.value.isBlank()) {
                _errorMessage.value = "Content cannot be empty"
                return@launch
            }

            _isSubmitting.value = true
            _errorMessage.value = null

            try {
                // Format scheduled date as ISO 8601 string if scheduling
                val scheduledAtString = if (_isScheduled.value && _scheduledDateTime.value != null) {
                    _scheduledDateTime.value?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                } else {
                    null
                }

                val response = postRepository.createCommunityPost(
                    content = _content.value,
                    profileId = profileId,
                    inReplyToId = inReplyToId,
                    imageIds = _uploadedImageIds.value.ifEmpty { null },
                    announcement = if (_isAnnouncement.value) true else null,
                    contentWarning = _contentWarning.value,
                    scheduledAt = scheduledAtString
                )

                if (response.isSuccessful) {
                    _createdPost.value = response.body()?.data
                    Log.d(TAG, "Created post: ${response.body()?.data?.id}")
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to create post: ${response.code()}"
                    Log.e(TAG, "Failed to create post: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error creating post: ${e.localizedMessage}"
                Log.e(TAG, "Failed to create post", e)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun reset() {
        _content.value = ""
        _selectedImages.value = emptyList()
        _uploadedImageIds.value = emptyList()
        _isAnnouncement.value = false
        _contentWarning.value = null
        _isScheduled.value = false
        _scheduledDateTime.value = null
        _errorMessage.value = null
        _createdPost.value = null
        _showMentionDropdown.value = false
        _mentionQuery.value = ""
        _mentionProfiles.value = emptyList()
        mentionStartIndex = -1
    }
}
