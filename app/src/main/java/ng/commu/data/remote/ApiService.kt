package ng.commu.data.remote

import ng.commu.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("/console/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/console/signup")
    suspend fun signup(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/console/me")
    suspend fun getCurrentUser(): Response<ApiResponse<User>>

    @POST("/auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("/console/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    @POST("/console/change-email")
    suspend fun changeEmail(@Body request: ChangeEmailRequest): Response<Unit>

    @POST("/console/request-data-export")
    suspend fun requestDataExport(): Response<Unit>

    @DELETE("/console/delete-account")
    suspend fun deleteAccount(): Response<Unit>

    @GET("/console/boards")
    suspend fun getBoards(): Response<BoardsListResponse>

    @GET("/console/board/{boardSlug}")
    suspend fun getBoard(@Path("boardSlug") boardSlug: String): Response<BoardResponse>

    @GET("/console/board/{boardSlug}/hashtags")
    suspend fun getBoardHashtags(@Path("boardSlug") boardSlug: String): Response<HashtagsResponse>

    @GET("/console/board/{boardSlug}/posts")
    suspend fun getPosts(
        @Path("boardSlug") boardSlug: String,
        @Query("hashtags") hashtags: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<PostsListResponse>

    @GET("/console/board/{boardSlug}/posts/{postId}")
    suspend fun getPost(
        @Path("boardSlug") boardSlug: String,
        @Path("postId") postId: String
    ): Response<PostDetailResponse>

    @Multipart
    @POST("/console/upload/file")
    suspend fun uploadImage(@Part file: okhttp3.MultipartBody.Part): Response<ImageUploadResponse>

    @POST("/console/board/{boardSlug}/posts")
    suspend fun createPost(
        @Path("boardSlug") boardSlug: String,
        @Body request: CreatePostRequest
    ): Response<PostResponse>

    @GET("/console/board/{boardSlug}/posts/{postId}/replies")
    suspend fun getReplies(
        @Path("boardSlug") boardSlug: String,
        @Path("postId") postId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<RepliesListResponse>

    @POST("/console/board/{boardSlug}/posts/{postId}/replies")
    suspend fun createReply(
        @Path("boardSlug") boardSlug: String,
        @Path("postId") postId: String,
        @Body request: CreateReplyRequest
    ): Response<BoardPostReplyResponse>

    @PATCH("/console/board/{boardSlug}/posts/{postId}/replies/{replyId}")
    suspend fun updateReply(
        @Path("boardSlug") boardSlug: String,
        @Path("postId") postId: String,
        @Path("replyId") replyId: String,
        @Body request: UpdateReplyRequest
    ): Response<BoardPostReplyResponse>

    @DELETE("/console/board/{boardSlug}/posts/{postId}")
    suspend fun deletePost(
        @Path("boardSlug") boardSlug: String,
        @Path("postId") postId: String
    ): Response<Unit>

    @DELETE("/console/board/{boardSlug}/posts/{postId}/replies/{replyId}")
    suspend fun deleteReply(
        @Path("boardSlug") boardSlug: String,
        @Path("postId") postId: String,
        @Path("replyId") replyId: String
    ): Response<Unit>

    @POST("/console/devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<DeviceResponse>

    @DELETE("/console/devices/{pushToken}")
    suspend fun deleteDevice(@Path("pushToken") pushToken: String): Response<Unit>

    @GET("/app/notifications")
    suspend fun getNotifications(
        @Query("profile_id") profileId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<NotificationResponse>

    @GET("/app/notifications/unread-count")
    suspend fun getUnreadCount(
        @Query("profile_id") profileId: String
    ): Response<UnreadCountResponse>

    @POST("/app/notifications/mark-all-read")
    suspend fun markAllNotificationsAsRead(
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @POST("/app/notifications/{notification_id}/read")
    suspend fun markNotificationAsRead(
        @Path("notification_id") notificationId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @GET("/console/communities/mine")
    suspend fun getUserCommunities(): Response<CommunitiesListResponse>

    // MARK: - App Endpoints (Community-scoped)

    // Posts
    @GET("/app/posts")
    suspend fun getCommunityPosts(
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CommunityPostsResponse>

    @GET("/app/posts/{postId}")
    suspend fun getCommunityPost(
        @Path("postId") postId: String,
        @Query("profile_id") profileId: String? = null
    ): Response<ApiResponse<CommunityPost>>

    @POST("/app/posts")
    suspend fun createCommunityPost(
        @Body request: PostCreateRequest
    ): Response<ApiResponse<CommunityPost>>

    @PATCH("/app/posts/{postId}")
    suspend fun updateCommunityPost(
        @Path("postId") postId: String,
        @Query("profile_id") profileId: String,
        @Body request: PostUpdateRequest
    ): Response<ApiResponse<CommunityPost>>

    @DELETE("/app/posts/{postId}")
    suspend fun deleteCommunityPost(
        @Path("postId") postId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @GET("/app/posts/search")
    suspend fun searchPosts(
        @Query("q") query: String,
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CommunityPostsResponse>

    @GET("/app/scheduled-posts")
    suspend fun getScheduledPosts(
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<ScheduledPostsResponse>

    // Reactions
    @POST("/app/posts/{postId}/reactions")
    suspend fun addReaction(
        @Path("postId") postId: String,
        @Body request: ReactionCreateRequest
    ): Response<ApiResponse<ReactionCreateResponse>>

    @DELETE("/app/posts/{postId}/reactions")
    suspend fun removeReaction(
        @Path("postId") postId: String,
        @Query("emoji") emoji: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    // Bookmarks
    @POST("/app/posts/{postId}/bookmark")
    suspend fun bookmarkPost(
        @Path("postId") postId: String,
        @Query("profile_id") profileId: String
    ): Response<ApiResponse<BookmarkCreateResponse>>

    @DELETE("/app/posts/{postId}/bookmark")
    suspend fun unbookmarkPost(
        @Path("postId") postId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @GET("/app/bookmarks")
    suspend fun getBookmarkedPosts(
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CommunityPostsResponse>

    // Pin/Unpin
    @POST("/app/posts/{postId}/pin")
    suspend fun pinPost(
        @Path("postId") postId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @DELETE("/app/posts/{postId}/pin")
    suspend fun unpinPost(
        @Path("postId") postId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    // Messages - Direct Messages
    @GET("/app/conversations")
    suspend fun getConversations(
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<ConversationsResponse>

    @GET("/app/conversations/{otherProfileId}")
    suspend fun getConversationThread(
        @Path("otherProfileId") otherProfileId: String,
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): Response<ConversationThreadResponse>

    @POST("/app/messages")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<ApiResponse<Message>>

    @POST("/app/conversations/{otherProfileId}/mark-read")
    suspend fun markConversationAsRead(
        @Path("otherProfileId") otherProfileId: String
    ): Response<Unit>

    @POST("/app/conversations/mark-all-read")
    suspend fun markAllConversationsAsRead(): Response<Unit>

    @GET("/app/conversations/unread-count")
    suspend fun getUnreadMessagesCount(
        @Query("profile_id") profileId: String
    ): Response<MessageUnreadCountResponse>

    @POST("/app/messages/{messageId}/reactions")
    suspend fun addReactionToMessage(
        @Path("messageId") messageId: String,
        @Body request: AddReactionRequest
    ): Response<MessageReaction>

    @DELETE("/app/messages/{messageId}/reactions")
    suspend fun removeReactionFromMessage(
        @Path("messageId") messageId: String,
        @Query("emoji") emoji: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    // Messages - Group Chats
    @GET("/app/group-chats")
    suspend fun getGroupChats(
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<GroupChatsResponse>

    @POST("/app/group-chats")
    suspend fun createGroupChat(
        @Body request: CreateGroupChatRequest
    ): Response<GroupChat>

    @GET("/app/group-chats/{groupChatId}")
    suspend fun getGroupChat(
        @Path("groupChatId") groupChatId: String,
        @Query("profile_id") profileId: String
    ): Response<GroupChat>

    @PATCH("/app/group-chats/{groupChatId}")
    suspend fun updateGroupChat(
        @Path("groupChatId") groupChatId: String,
        @Query("profile_id") profileId: String,
        @Body request: UpdateGroupChatRequest
    ): Response<GroupChat>

    @DELETE("/app/group-chats/{groupChatId}")
    suspend fun deleteGroupChat(
        @Path("groupChatId") groupChatId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @GET("/app/group-chats/{groupChatId}/messages")
    suspend fun getGroupChatMessages(
        @Path("groupChatId") groupChatId: String,
        @Query("profile_id") profileId: String,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): Response<GroupChatMessagesResponse>

    @POST("/app/group-chats/{groupChatId}/messages")
    suspend fun sendGroupChatMessage(
        @Path("groupChatId") groupChatId: String,
        @Query("profile_id") profileId: String,
        @Body request: SendGroupMessageRequest
    ): Response<ApiResponse<GroupChatMessage>>

    @POST("/app/group-chats/{groupChatId}/members")
    suspend fun addGroupChatMember(
        @Path("groupChatId") groupChatId: String,
        @Query("profile_id") profileId: String,
        @Body request: AddGroupChatMemberRequest
    ): Response<Unit>

    @DELETE("/app/group-chats/{groupChatId}/members/{memberProfileId}")
    suspend fun removeGroupChatMember(
        @Path("groupChatId") groupChatId: String,
        @Path("memberProfileId") memberProfileId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @POST("/app/group-chats/{groupChatId}/leave")
    suspend fun leaveGroupChat(
        @Path("groupChatId") groupChatId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @POST("/app/group-chats/{groupChatId}/mark-read")
    suspend fun markGroupChatAsRead(
        @Path("groupChatId") groupChatId: String,
        @Query("profile_id") profileId: String
    ): Response<Unit>

    // Profiles
    @GET("/app/me/profiles")
    suspend fun getMyProfiles(): Response<ApiResponse<List<Profile>>>

    @GET("/app/profiles")
    suspend fun getCommunityProfiles(): Response<ApiResponse<List<Profile>>>

    @GET("/app/profiles/{username}")
    suspend fun getProfileByUsername(
        @Path("username") username: String,
        @Query("profile_id") profileId: String?
    ): Response<ApiResponse<Profile>>

    @GET("/app/profiles/{username}/posts")
    suspend fun getProfilePosts(
        @Path("username") username: String,
        @Query("profile_id") profileId: String?,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CommunityPostsResponse>

    @GET("/app/announcements")
    suspend fun getAnnouncements(
        @Query("profile_id") profileId: String?
    ): Response<ApiResponse<List<CommunityPost>>>

    @POST("/app/profiles")
    suspend fun createProfile(
        @Body request: CreateProfileRequest
    ): Response<ApiResponse<Profile>>

    @PUT("/app/profiles")
    suspend fun updateProfile(
        @Query("profile_id") profileId: String,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<Profile>>

    @POST("/app/profiles/set-primary")
    suspend fun setPrimaryProfile(
        @Query("profile_id") profileId: String
    ): Response<Unit>

    @Multipart
    @POST("/app/profile-picture")
    suspend fun uploadProfilePicture(
        @Part file: okhttp3.MultipartBody.Part
    ): Response<ProfilePicture>

    @GET("/app/profiles/online-status")
    suspend fun getOnlineStatus(
        @Query("profile_ids") profileIds: String
    ): Response<OnlineStatusResponse>

    @PUT("/app/profiles/online-status-settings")
    suspend fun updateOnlineStatusVisibility(
        @Body request: OnlineStatusVisibilityRequest
    ): Response<Unit>

    // Profile Sharing
    @GET("/app/profiles/{profileId}/users")
    suspend fun getSharedUsers(
        @Path("profileId") profileId: String
    ): Response<SharedUsersResponse>

    @POST("/app/profiles/{profileId}/users")
    suspend fun shareProfile(
        @Path("profileId") profileId: String,
        @Body request: ProfileShareRequest
    ): Response<Unit>

    @DELETE("/app/profiles/{profileId}/shared-profiles/{sharedProfileId}")
    suspend fun removeSharedUser(
        @Path("profileId") profileId: String,
        @Path("sharedProfileId") sharedProfileId: String
    ): Response<Unit>
}
