package ng.commu.data.model

import com.squareup.moshi.Json

data class Community(
    val id: String,
    val name: String,
    val slug: String,
    @Json(name = "starts_at") val startsAt: String,
    @Json(name = "ends_at") val endsAt: String,
    @Json(name = "is_recruiting") val isRecruiting: Boolean,
    @Json(name = "recruiting_starts_at") val recruitingStartsAt: String?,
    @Json(name = "recruiting_ends_at") val recruitingEndsAt: String?,
    @Json(name = "minimum_birth_year") val minimumBirthYear: Int?,
    @Json(name = "created_at") val createdAt: String,
    val role: String?,
    @Json(name = "custom_domain") val customDomain: String?,
    @Json(name = "domain_verified") val domainVerified: String?,
    @Json(name = "banner_image_url") val bannerImageUrl: String?,
    @Json(name = "banner_image_width") val bannerImageWidth: Int?,
    @Json(name = "banner_image_height") val bannerImageHeight: Int?,
    val hashtags: List<CommunityHashtag>,
    @Json(name = "owner_profile_id") val ownerProfileId: String?,
    @Json(name = "pending_application_count") val pendingApplicationCount: Int?
) {
    val communityUrl: String
        get() = if (customDomain != null && domainVerified != null) {
            "https://$customDomain"
        } else {
            "https://$slug.commu.ng"
        }
}

data class CommunityHashtag(
    val id: String,
    val tag: String
)

data class CommunitiesListResponse(
    val data: List<Community>
) {
    val communities: List<Community>
        get() = data
}

data class CommunityDetails(
    val id: String,
    val name: String,
    val slug: String,
    @Json(name = "starts_at") val startsAt: String,
    @Json(name = "ends_at") val endsAt: String,
    @Json(name = "is_recruiting") val isRecruiting: Boolean,
    @Json(name = "recruiting_starts_at") val recruitingStartsAt: String?,
    @Json(name = "recruiting_ends_at") val recruitingEndsAt: String?,
    val description: String?,
    @Json(name = "custom_domain") val customDomain: String?,
    @Json(name = "domain_verified") val domainVerified: String?,
    @Json(name = "banner_image_url") val bannerImageUrl: String?,
    val hashtags: List<CommunityHashtag>,
    @Json(name = "membership_status") val membershipStatus: String?
) {
    val communityUrl: String
        get() = if (customDomain != null && domainVerified != null) {
            "https://$customDomain"
        } else {
            "https://$slug.commu.ng"
        }
}

data class CommunityDetailsResponse(
    val data: CommunityDetails
)

data class CommunityLink(
    val id: String,
    val title: String,
    val url: String,
    @Json(name = "created_at") val createdAt: String
)

data class CommunityLinksResponse(
    val data: List<CommunityLink>
)

data class CommunityApplication(
    val id: String,
    val status: String,
    @Json(name = "profile_name") val profileName: String,
    @Json(name = "profile_username") val profileUsername: String,
    val message: String?,
    @Json(name = "rejection_reason") val rejectionReason: String?,
    @Json(name = "created_at") val createdAt: String
)

data class CommunityApplicationsResponse(
    val data: List<CommunityApplication>
)

data class CommunityApplicationResponse(
    val data: CommunityApplication
)

data class ApplyToCommunityRequest(
    @Json(name = "profile_name") val profileName: String,
    @Json(name = "profile_username") val profileUsername: String,
    val message: String?
)

data class CommunityApplicationDetail(
    val id: String,
    val status: String,
    @Json(name = "profile_name") val profileName: String,
    @Json(name = "profile_username") val profileUsername: String,
    val message: String?,
    @Json(name = "rejection_reason") val rejectionReason: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "reviewed_at") val reviewedAt: String?
)

data class CommunityApplicationsDetailResponse(
    val data: List<CommunityApplicationDetail>
)

data class ApplicationReviewRequest(
    val status: String,
    @Json(name = "rejection_reason") val rejectionReason: String? = null
)

data class ApplicationReviewResponse(
    val id: String,
    val status: String,
    @Json(name = "reviewed_at") val reviewedAt: String?,
    @Json(name = "membership_id") val membershipId: String?,
    @Json(name = "profile_id") val profileId: String?
)

data class ApplicationReviewResponseWrapper(
    val data: ApplicationReviewResponse
)

data class CommunityCreateRequest(
    val name: String,
    val slug: String,
    @Json(name = "starts_at") val startsAt: String,
    @Json(name = "ends_at") val endsAt: String,
    @Json(name = "is_recruiting") val isRecruiting: Boolean,
    @Json(name = "recruiting_starts_at") val recruitingStartsAt: String?,
    @Json(name = "recruiting_ends_at") val recruitingEndsAt: String?,
    @Json(name = "minimum_birth_year") val minimumBirthYear: Int?,
    @Json(name = "image_id") val imageId: String?,
    val hashtags: List<String>?,
    @Json(name = "profile_username") val profileUsername: String,
    @Json(name = "profile_name") val profileName: String,
    val description: String?,
    @Json(name = "mute_new_members") val muteNewMembers: Boolean?
)

data class CommunityCreateResponse(
    val id: String,
    val name: String,
    val domain: String,
    @Json(name = "starts_at") val startsAt: String,
    @Json(name = "ends_at") val endsAt: String,
    @Json(name = "is_recruiting") val isRecruiting: Boolean,
    @Json(name = "owner_profile_id") val ownerProfileId: String,
    @Json(name = "created_at") val createdAt: String
)

data class CommunityCreateResponseWrapper(
    val data: CommunityCreateResponse
)

data class CommunityUpdateRequest(
    val name: String,
    val slug: String,
    @Json(name = "starts_at") val startsAt: String,
    @Json(name = "ends_at") val endsAt: String,
    @Json(name = "is_recruiting") val isRecruiting: Boolean,
    @Json(name = "recruiting_starts_at") val recruitingStartsAt: String?,
    @Json(name = "recruiting_ends_at") val recruitingEndsAt: String?,
    @Json(name = "minimum_birth_year") val minimumBirthYear: Int?,
    @Json(name = "image_id") val imageId: String?,
    val hashtags: List<String>?,
    val description: String?,
    @Json(name = "mute_new_members") val muteNewMembers: Boolean?
)

data class CommunityUpdateResponse(
    val id: String,
    val name: String,
    val slug: String,
    @Json(name = "starts_at") val startsAt: String,
    @Json(name = "ends_at") val endsAt: String,
    @Json(name = "is_recruiting") val isRecruiting: Boolean,
    @Json(name = "recruiting_starts_at") val recruitingStartsAt: String?,
    @Json(name = "recruiting_ends_at") val recruitingEndsAt: String?
)

data class CommunityUpdateResponseWrapper(
    val data: CommunityUpdateResponse
)
