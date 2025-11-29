package ng.commu.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.CommunityPost
import ng.commu.data.model.CommunityPostImage
import ng.commu.data.repository.PostRepository
import javax.inject.Inject

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    data class EditImage(
        val id: String,
        val url: String?,
        val uri: Uri?,
        val isNew: Boolean
    )

    private val _post = MutableStateFlow<CommunityPost?>(null)
    val post: StateFlow<CommunityPost?> = _post.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _contentWarning = MutableStateFlow("")
    val contentWarning: StateFlow<String> = _contentWarning.asStateFlow()

    private val _images = MutableStateFlow<List<EditImage>>(emptyList())
    val images: StateFlow<List<EditImage>> = _images.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var originalImageIds: Set<String> = emptySet()
    private var uploadedImageIds: MutableList<String> = mutableListOf()

    fun loadPost(post: CommunityPost) {
        _post.value = post
        _content.value = post.content
        _contentWarning.value = post.contentWarning ?: ""

        // Load existing images
        _images.value = (post.images ?: emptyList()).map { image ->
            EditImage(
                id = image.id,
                url = image.url,
                uri = null,
                isNew = false
            )
        }
        originalImageIds = (post.images ?: emptyList()).map { it.id }.toSet()
    }

    fun updateContent(content: String) {
        _content.value = content
    }

    fun updateContentWarning(warning: String) {
        _contentWarning.value = warning
    }

    fun addImage(uri: Uri, imageData: ByteArray, filename: String) {
        // Add to UI immediately with temp ID
        val tempId = "temp_${System.currentTimeMillis()}"
        _images.value = _images.value + EditImage(
            id = tempId,
            url = null,
            uri = uri,
            isNew = true
        )

        // Upload in background
        viewModelScope.launch {
            uploadImage(imageData, filename, tempId)
        }
    }

    private suspend fun uploadImage(imageData: ByteArray, filename: String, tempId: String) {
        _isUploadingImage.value = true
        _errorMessage.value = null

        try {
            val response = postRepository.uploadPostImage(imageData, filename)

            if (response.isSuccessful) {
                response.body()?.data?.let { result ->
                    // Replace temp image with uploaded one
                    _images.value = _images.value.map { image ->
                        if (image.id == tempId) {
                            EditImage(
                                id = result.id,
                                url = result.url,
                                uri = null,
                                isNew = true
                            )
                        } else {
                            image
                        }
                    }
                    uploadedImageIds.add(result.id)
                }
            } else {
                _errorMessage.value = "Failed to upload image: ${response.code()}"
                _images.value = _images.value.filter { it.id != tempId }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to upload image: ${e.message}"
            // Remove failed upload
            _images.value = _images.value.filter { it.id != tempId }
        } finally {
            _isUploadingImage.value = false
        }
    }

    fun removeImage(image: EditImage) {
        _images.value = _images.value.filter { it.id != image.id }
        uploadedImageIds.remove(image.id)
    }

    fun hasChanges(): Boolean {
        val post = _post.value ?: return false
        val currentImageIds = _images.value.map { it.id }.toSet()

        return _content.value != post.content ||
                _contentWarning.value != (post.contentWarning ?: "") ||
                currentImageIds != originalImageIds
    }

    fun canSave(): Boolean {
        return _content.value.trim().isNotEmpty() &&
                _content.value.length <= 500 &&
                !_isSaving.value &&
                hasChanges()
    }

    fun savePost(postId: String, profileId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            try {
                val imageIds = _images.value.map { it.id }
                val warning = _contentWarning.value.trim()

                postRepository.updateCommunityPost(
                    postId = postId,
                    profileId = profileId,
                    content = _content.value.trim(),
                    imageIds = if (imageIds.isEmpty()) null else imageIds,
                    contentWarning = if (warning.isEmpty()) null else warning
                )

                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update post: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
