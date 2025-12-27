package ng.commu.data.model

import com.squareup.moshi.Json

// MARK: - Direct Messages

data class Message(
    val id: String,
    @Json(name = "sender_id") val senderId: String? = null,
    @Json(name = "receiver_id") val receiverId: String? = null,
    val content: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "read_at") val readAt: String? = null,
    val sender: CommunityPostAuthor? = null,
    val receiver: CommunityPostAuthor? = null,
    val images: List<MessageImage> = emptyList(),
    val reactions: List<MessageReaction> = emptyList()
)

data class MessageImage(
    val id: String,
    val url: String,
    val width: Int?,
    val height: Int?
)

data class MessageReaction(
    val id: String? = null,
    val emoji: String,
    @Json(name = "user") val profile: CommunityPostAuthor? = null
)

// MARK: - Conversations

data class Conversation(
    @Json(name = "other_profile") val otherProfile: CommunityPostAuthor,
    @Json(name = "last_message") val lastMessage: Message?,
    @Json(name = "unread_count") val unreadCount: String
)

data class ConversationsResponse(
    val data: List<Conversation>,
    val pagination: ApiPagination
)

data class ConversationThreadResponse(
    val data: List<Message>,
    val pagination: ApiPagination
)

// MARK: - Group Chats

data class GroupChat(
    val id: String,
    val name: String,
    val description: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "created_by_profile_id") val createdByProfileId: String?,
    @Json(name = "member_count") val memberCount: Int?,
    val members: List<CommunityPostAuthor>,
    @Json(name = "last_message") val lastMessage: GroupChatMessage?,
    @Json(name = "unread_count") val unreadCount: Int
)

data class GroupChatMessage(
    val id: String,
    @Json(name = "group_chat_id") val groupChatId: String?,
    @Json(name = "sender_profile_id") val senderProfileId: String?,
    val content: String,
    @Json(name = "created_at") val createdAt: String,
    val sender: CommunityPostAuthor?,
    val images: List<MessageImage>,
    val reactions: List<MessageReaction>
)

data class GroupChatsResponse(
    val data: List<GroupChat>,
    val pagination: ApiPagination
)

data class GroupChatMessagesResponse(
    val data: List<GroupChatMessage>,
    val pagination: ApiPagination
)

// MARK: - Request Types

data class SendMessageRequest(
    val content: String,
    @Json(name = "receiver_id") val receiverId: String,
    @Json(name = "profile_id") val profileId: String,
    @Json(name = "image_ids") val imageIds: List<String>?
)

data class SendGroupMessageRequest(
    val content: String,
    @Json(name = "image_ids") val imageIds: List<String>?
)

data class CreateGroupChatRequest(
    val name: String,
    @Json(name = "creator_profile_id") val creatorProfileId: String,
    @Json(name = "member_profile_ids") val memberProfileIds: List<String>
)

data class UpdateGroupChatRequest(
    val name: String,
    val description: String?
)

data class AddGroupChatMemberRequest(
    @Json(name = "profile_id") val profileId: String
)

data class AddReactionRequest(
    val emoji: String,
    @Json(name = "profile_id") val profileId: String
)

// MARK: - Unread Count

data class MessageUnreadCountData(
    val count: Int
)

data class MessageUnreadCountResponse(
    val data: MessageUnreadCountData
)
