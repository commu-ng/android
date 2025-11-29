package ng.commu.data.repository

import ng.commu.data.model.*
import ng.commu.data.remote.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val apiService: ApiService
) {
    // MARK: - Posts

    suspend fun getCommunityPosts(
        profileId: String,
        limit: Int = 20,
        cursor: String? = null
    ) = apiService.getCommunityPosts(profileId, limit, cursor)

    suspend fun getCommunityPost(
        postId: String,
        profileId: String? = null
    ) = apiService.getCommunityPost(postId, profileId)

    suspend fun createCommunityPost(
        content: String,
        profileId: String,
        inReplyToId: String? = null,
        imageIds: List<String>? = null,
        announcement: Boolean? = null,
        contentWarning: String? = null,
        scheduledAt: String? = null
    ) = apiService.createCommunityPost(
        PostCreateRequest(
            content = content,
            profileId = profileId,
            inReplyToId = inReplyToId,
            imageIds = imageIds,
            announcement = announcement,
            contentWarning = contentWarning,
            scheduledAt = scheduledAt
        )
    )

    suspend fun updateCommunityPost(
        postId: String,
        profileId: String,
        content: String,
        imageIds: List<String>? = null,
        contentWarning: String? = null
    ) = apiService.updateCommunityPost(
        postId,
        profileId,
        PostUpdateRequest(
            content = content,
            imageIds = imageIds,
            contentWarning = contentWarning
        )
    )

    suspend fun deleteCommunityPost(
        postId: String,
        profileId: String
    ) = apiService.deleteCommunityPost(postId, profileId)

    suspend fun reportPost(
        postId: String,
        profileId: String,
        reason: String
    ) = apiService.reportPost(postId, ReportPostRequest(reason, profileId))

    suspend fun searchPosts(
        query: String,
        profileId: String,
        limit: Int = 20,
        cursor: String? = null
    ) = apiService.searchPosts(query, profileId, limit, cursor)

    suspend fun getScheduledPosts(
        profileId: String,
        limit: Int = 20,
        cursor: String? = null
    ) = apiService.getScheduledPosts(profileId, limit, cursor)

    // MARK: - Reactions

    suspend fun addReaction(
        postId: String,
        emoji: String,
        profileId: String
    ) = apiService.addReaction(postId, ReactionCreateRequest(emoji, profileId))

    suspend fun removeReaction(
        postId: String,
        emoji: String,
        profileId: String
    ) = apiService.removeReaction(postId, emoji, profileId)

    // MARK: - Bookmarks

    suspend fun bookmarkPost(
        postId: String,
        profileId: String
    ) = apiService.bookmarkPost(postId, profileId)

    suspend fun unbookmarkPost(
        postId: String,
        profileId: String
    ) = apiService.unbookmarkPost(postId, profileId)

    suspend fun getBookmarkedPosts(
        profileId: String,
        limit: Int = 20,
        cursor: String? = null
    ) = apiService.getBookmarkedPosts(profileId, limit, cursor)

    // MARK: - Pin/Unpin

    suspend fun pinPost(
        postId: String,
        profileId: String
    ) = apiService.pinPost(postId, profileId)

    suspend fun unpinPost(
        postId: String,
        profileId: String
    ) = apiService.unpinPost(postId, profileId)

    // MARK: - Image Upload

    suspend fun uploadImage(file: File) = apiService.uploadImage(
        MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        )
    )

    suspend fun uploadPostImage(imageData: ByteArray, filename: String) = apiService.uploadImage(
        MultipartBody.Part.createFormData(
            "file",
            filename,
            imageData.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        )
    )
}
