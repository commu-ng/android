package ng.commu.data.model

import com.squareup.moshi.Json

// MARK: - App Profile (Community-scoped)

data class Profile(
    val id: String,
    val name: String,
    val username: String,
    val bio: String?,
    @Json(name = "profile_picture_url") val profilePictureUrl: String?,
    @Json(name = "community_id") val communityId: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "is_primary") val isPrimary: Boolean,
    @Json(name = "is_online") val isOnline: Boolean?,
    @Json(name = "is_active") val isActive: Boolean?,
    val role: String?
)

// MARK: - Profile Requests

data class CreateProfileRequest(
    val name: String,
    val username: String,
    val bio: String?,
    @Json(name = "community_id") val communityId: String
)

data class UpdateProfileRequest(
    val name: String?,
    val bio: String?
)

// MARK: - Profile Picture

data class ProfilePicture(
    val id: String,
    val url: String,
    val width: Int,
    val height: Int
)

// MARK: - Online Status

data class OnlineStatusResponse(
    val data: Map<String, Boolean>
)

data class OnlineStatusVisibilityRequest(
    val visibility: String,
    @Json(name = "profile_id") val profileId: String
)

// MARK: - Profile Sharing (Multi-profile management)

data class SharedUser(
    val id: String,
    @Json(name = "login_name") val loginName: String,
    @Json(name = "display_name") val displayName: String
)

data class SharedUsersResponse(
    val data: List<SharedUser>
)

data class ProfileShareRequest(
    val username: String,
    val role: String
)
