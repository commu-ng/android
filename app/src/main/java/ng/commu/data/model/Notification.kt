package ng.commu.data.model

import com.squareup.moshi.Json

data class Notification(
    val id: String,
    val type: String?, // "reply", "mention", "reaction"
    val content: String?,
    @Json(name = "read_at") val readAt: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "community_url") val communityUrl: String?,
    @Json(name = "community_name") val communityName: String?,
    val sender: NotificationSender?,
    @Json(name = "related_post") val relatedPost: NotificationRelatedPost?
)

data class NotificationSender(
    val id: String,
    val name: String,
    val username: String,
    @Json(name = "profile_picture_url") val profilePictureUrl: String?
)

data class NotificationRelatedPost(
    val id: String,
    val content: String,
    val author: NotificationAuthor
)

data class NotificationAuthor(
    val id: String,
    val name: String,
    val username: String,
    @Json(name = "profile_picture_url") val profilePictureUrl: String?
)

data class NotificationResponse(
    val data: List<Notification>,
    val pagination: ApiPagination
)

data class UnreadCountResponse(
    val count: Int
)
