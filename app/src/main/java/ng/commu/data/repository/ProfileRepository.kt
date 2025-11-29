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
class ProfileRepository @Inject constructor(
    private val apiService: ApiService
) {
    // MARK: - Profile Management

    suspend fun getMyProfiles() =
        apiService.getMyProfiles()

    suspend fun getCommunityProfiles() =
        apiService.getCommunityProfiles()

    suspend fun getAllProfiles(profileId: String) =
        apiService.getCommunityProfiles()

    suspend fun getProfileByUsername(username: String, profileId: String?) =
        apiService.getProfileByUsername(username, profileId)

    suspend fun getProfilePosts(
        username: String,
        profileId: String?,
        limit: Int = 20,
        cursor: String? = null
    ) = apiService.getProfilePosts(username, profileId, limit, cursor)

    suspend fun createProfile(
        name: String,
        username: String,
        bio: String?,
        communityId: String
    ) = apiService.createProfile(
        CreateProfileRequest(
            name = name,
            username = username,
            bio = bio,
            communityId = communityId
        )
    )

    suspend fun updateProfile(
        profileId: String,
        name: String?,
        bio: String?
    ) = apiService.updateProfile(
        profileId,
        UpdateProfileRequest(name, bio)
    )

    suspend fun setPrimaryProfile(
        profileId: String
    ) = apiService.setPrimaryProfile(profileId)

    // MARK: - Profile Picture

    suspend fun uploadProfilePicture(file: File) =
        apiService.uploadProfilePicture(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
        )

    suspend fun uploadProfilePicture(imageData: ByteArray, filename: String) =
        apiService.uploadProfilePicture(
            MultipartBody.Part.createFormData(
                "file",
                filename,
                imageData.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
        )

    // MARK: - Online Status

    suspend fun getOnlineStatus(
        profileIds: List<String>
    ) = apiService.getOnlineStatus(profileIds.joinToString(","))

    suspend fun updateOnlineStatusVisibility(
        visibility: String,
        profileId: String
    ) = apiService.updateOnlineStatusVisibility(
        OnlineStatusVisibilityRequest(visibility, profileId)
    )

    // MARK: - Profile Sharing

    suspend fun getSharedUsers(
        profileId: String
    ) = apiService.getSharedUsers(profileId)

    suspend fun shareProfile(
        profileId: String,
        userLoginName: String
    ) = apiService.shareProfile(
        profileId,
        ProfileShareRequest(profileId, userLoginName)
    )

    suspend fun removeSharedUser(
        profileId: String,
        sharedProfileId: String
    ) = apiService.removeSharedUser(profileId, sharedProfileId)
}
