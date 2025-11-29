package ng.commu.data.model

import com.squareup.moshi.Json

data class Board(
    val id: String,
    val name: String?,
    val slug: String?,
    val description: String?,
    @Json(name = "allow_comments") val allowComments: Boolean,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

data class BoardsListResponse(
    val data: List<Board>
) {
    val boards: List<Board>
        get() = data
}

data class BoardResponse(
    val data: Board
)

data class HashtagsResponse(
    val data: List<String>
)

data class Post(
    val id: String,
    val title: String,
    val content: String?,
    val image: PostImage?,
    val hashtags: List<PostHashtag>,
    val author: PostAuthor,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

data class PostImage(
    val id: String,
    val url: String,
    val width: Int,
    val height: Int,
    val filename: String
)

data class PostHashtag(
    val id: String,
    val tag: String
)

data class PostAuthor(
    val id: String,
    @Json(name = "login_name") val loginName: String,
    @Json(name = "avatar_url") val avatarUrl: String?
)

data class PostsListResponse(
    val data: List<Post>,
    val pagination: ApiPagination
) {
    val posts: List<Post>
        get() = data

    val nextCursor: String?
        get() = pagination.nextCursor

    val hasMore: Boolean
        get() = pagination.hasMore
}

data class PostDetailResponse(
    val data: Post
) {
    val post: Post
        get() = data
}

data class Comment(
    val id: String,
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "parent_comment_id") val parentCommentId: String?,
    @Json(name = "body_text") val bodyText: String?,
    @Json(name = "body_html") val bodyHTML: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "deleted_at") val deletedAt: String?,
    val user: CommentUser
)

data class CommentUser(
    val id: String,
    @Json(name = "login_name") val loginName: String,
    @Json(name = "avatar_url") val avatarUrl: String?
)

data class CommentsListResponse(
    val comments: List<Comment>
)

data class BoardPostReply(
    val id: String,
    val content: String,
    val depth: Int,
    val author: PostAuthor,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    val replies: List<BoardPostReply>? = null
)

data class RepliesListResponse(
    val data: List<BoardPostReply>,
    val pagination: ApiPagination
) {
    val replies: List<BoardPostReply>
        get() = data

    val nextCursor: String?
        get() = pagination.nextCursor

    val hasMore: Boolean
        get() = pagination.hasMore
}

data class CreatePostRequest(
    val title: String,
    val content: String,
    @Json(name = "image_id") val imageId: String? = null,
    val hashtags: List<String>? = null
)

data class PostResponse(
    val data: Post
)

data class ImageUploadResponse(
    val id: String,
    val url: String,
    val width: Int,
    val height: Int,
    val filename: String
)

data class ImageUploadResponseWrapper(
    val data: ImageUploadResponse
)

data class BoardPostReplyResponse(
    val data: BoardPostReply
)

data class CreateReplyRequest(
    val content: String,
    @Json(name = "in_reply_to_id") val inReplyToId: String? = null
)

data class UpdateReplyRequest(
    val content: String
)

data class ReportBoardPostRequest(
    val reason: String
)
