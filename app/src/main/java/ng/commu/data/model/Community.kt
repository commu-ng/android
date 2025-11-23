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
    val role: String,
    @Json(name = "custom_domain") val customDomain: String?,
    @Json(name = "domain_verified") val domainVerified: String?,
    @Json(name = "banner_image_url") val bannerImageUrl: String?,
    @Json(name = "banner_image_width") val bannerImageWidth: Int?,
    @Json(name = "banner_image_height") val bannerImageHeight: Int?,
    val hashtags: List<CommunityHashtag>,
    @Json(name = "owner_profile_id") val ownerProfileId: String?,
    @Json(name = "pending_application_count") val pendingApplicationCount: Int
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
