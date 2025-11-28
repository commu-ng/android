package ng.commu.data.model

import com.squareup.moshi.Json

// MARK: - API Response Wrappers

data class ApiResponse<T>(
    val data: T
)

data class ApiPagination(
    @Json(name = "next_cursor") val nextCursor: String?,
    @Json(name = "has_more") val hasMore: Boolean,
    @Json(name = "total_count") val totalCount: Int?
)

// MARK: - Community Post Models

// Note: Using a separate type for parent post to avoid recursive data class issue
data class CommunityPostParent(
    val id: String,
    val content: String,
    val author: CommunityPostAuthor,
    @Json(name = "created_at") val createdAt: String,
    val images: List<CommunityPostImage>
)

data class CommunityPost(
    val id: String,
    val content: String,
    val author: CommunityPostAuthor,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "in_reply_to_id") val inReplyToId: String?,
    @Json(name = "parent_post") val parentPost: CommunityPostParent?,
    @Json(name = "parent_thread") val parentThread: List<CommunityPostParent>?,
    val replies: List<CommunityPost>?,
    val reactions: List<CommunityPostReaction>?,
    val images: List<CommunityPostImage>?,
    val announcement: Boolean?,
    @Json(name = "content_warning") val contentWarning: String?,
    @Json(name = "scheduled_at") val scheduledAt: String?,
    @Json(name = "pinned_at") val pinnedAt: String?,
    @Json(name = "is_bookmarked") val isBookmarked: Boolean?,
    val depth: Int?,
    @Json(name = "root_post_id") val rootPostId: String?
) {
    val replyCount: Int
        get() = countTotalReplies(replies ?: emptyList())

    val reactionCount: Int
        get() = (reactions ?: emptyList()).size

    val isPinned: Boolean
        get() = pinnedAt != null

    private fun countTotalReplies(replies: List<CommunityPost>): Int {
        var count = replies.size
        for (reply in replies) {
            count += countTotalReplies(reply.replies ?: emptyList())
        }
        return count
    }
}

data class CommunityPostAuthor(
    val id: String,
    val name: String,
    val username: String,
    val bio: String?,
    @Json(name = "profile_picture_url") val profilePictureUrl: String?,
    @Json(name = "is_online") val isOnline: Boolean?
)

data class CommunityPostImage(
    val id: String,
    val url: String,
    val width: Int?,
    val height: Int?
)

data class CommunityPostReactionUser(
    val id: String,
    val username: String,
    val name: String
)

data class CommunityPostReaction(
    val emoji: String,
    val user: CommunityPostReactionUser?
)

// MARK: - Post List Response

data class CommunityPostsResponse(
    val data: List<CommunityPost>,
    val pagination: ApiPagination
)

// MARK: - Post Create Request

data class PostCreateRequest(
    val content: String,
    @Json(name = "profile_id") val profileId: String,
    @Json(name = "in_reply_to_id") val inReplyToId: String?,
    @Json(name = "image_ids") val imageIds: List<String>?,
    val announcement: Boolean?,
    @Json(name = "content_warning") val contentWarning: String?,
    @Json(name = "scheduled_at") val scheduledAt: String?
)

// MARK: - Post Update Request

data class PostUpdateRequest(
    val content: String,
    @Json(name = "image_ids") val imageIds: List<String>?,
    @Json(name = "content_warning") val contentWarning: String?
)

// MARK: - Scheduled Posts Response

data class ScheduledPostsResponse(
    val data: List<CommunityPost>,
    val pagination: ApiPagination
)

// MARK: - Post History

data class PostHistory(
    val id: String,
    @Json(name = "post_id") val postId: String,
    val content: String,
    @Json(name = "edited_at") val editedAt: String,
    @Json(name = "edited_by") val editedBy: String
)

// MARK: - Bookmark

data class Bookmark(
    val id: String,
    @Json(name = "post_id") val postId: String,
    @Json(name = "profile_id") val profileId: String,
    @Json(name = "created_at") val createdAt: String
)

data class BookmarkCreateResponse(
    val message: String,
    @Json(name = "bookmark_id") val bookmarkId: String
)

// MARK: - Reaction Create

data class ReactionCreateRequest(
    val emoji: String,
    @Json(name = "profile_id") val profileId: String
)

data class ReactionCreateResponse(
    val id: String,
    val message: String,
    val emoji: String
)

// MARK: - Report Post

data class ReportPostRequest(
    val reason: String,
    @Json(name = "profile_id") val profileId: String
)

// MARK: - Export Job

data class ExportJob(
    val id: String,
    val status: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "download_url") val downloadUrl: String?
)
