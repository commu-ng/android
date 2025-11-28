package ng.commu.data.repository

import ng.commu.data.model.*
import ng.commu.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getBoards(): Result<List<Board>> {
        return try {
            val response = apiService.getBoards()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.boards)
            } else {
                Result.failure(Exception("Failed to fetch boards: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBoard(boardSlug: String): Result<Board> {
        return try {
            val response = apiService.getBoard(boardSlug)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch board: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBoardHashtags(boardSlug: String): Result<List<String>> {
        return try {
            val response = apiService.getBoardHashtags(boardSlug)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch hashtags: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPosts(
        boardSlug: String,
        hashtags: List<String>? = null,
        cursor: String? = null,
        limit: Int = 20
    ): Result<PostsListResponse> {
        return try {
            val hashtagsParam = hashtags?.joinToString(",")
            val response = apiService.getPosts(boardSlug, hashtagsParam, cursor, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch posts: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPost(boardSlug: String, postId: String): Result<Post> {
        return try {
            val response = apiService.getPost(boardSlug, postId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.post)
            } else {
                Result.failure(Exception("Failed to fetch post: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReplies(
        boardSlug: String,
        postId: String,
        cursor: String? = null,
        limit: Int = 20
    ): Result<RepliesListResponse> {
        return try {
            val response = apiService.getReplies(boardSlug, postId, cursor, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch replies: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReply(
        boardSlug: String,
        postId: String,
        content: String,
        inReplyToId: String? = null
    ): Result<BoardPostReplyResponse> {
        return try {
            val request = CreateReplyRequest(
                content = content,
                inReplyToId = inReplyToId
            )
            val response = apiService.createReply(boardSlug, postId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create reply: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReply(
        boardSlug: String,
        postId: String,
        replyId: String,
        content: String
    ): Result<BoardPostReplyResponse> {
        return try {
            val request = UpdateReplyRequest(content = content)
            val response = apiService.updateReply(boardSlug, postId, replyId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update reply: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReply(
        boardSlug: String,
        postId: String,
        replyId: String
    ): Result<Unit> {
        return try {
            val response = apiService.deleteReply(boardSlug, postId, replyId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete reply: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(
        boardSlug: String,
        postId: String
    ): Result<Unit> {
        return try {
            val response = apiService.deletePost(boardSlug, postId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete post: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(file: okhttp3.MultipartBody.Part): Result<ImageUploadResponse> {
        return try {
            val response = apiService.uploadImage(file)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to upload image: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(
        boardSlug: String,
        title: String,
        content: String,
        imageId: String? = null,
        hashtags: List<String>? = null
    ): Result<Post> {
        return try {
            val request = CreatePostRequest(
                title = title,
                content = content,
                imageId = imageId,
                hashtags = hashtags
            )
            val response = apiService.createPost(boardSlug, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to create post: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportPost(
        boardSlug: String,
        postId: String,
        reason: String
    ): Result<Unit> {
        return try {
            val request = ReportBoardPostRequest(reason = reason)
            val response = apiService.reportBoardPost(boardSlug, postId, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to report post: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
