package ng.commu.data.repository

import ng.commu.data.model.*
import ng.commu.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: ApiService
) {
    // MARK: - Direct Messages

    suspend fun getConversations(
        profileId: String,
        limit: Int = 20,
        cursor: String? = null
    ) = apiService.getConversations(profileId, limit, cursor)

    suspend fun getConversationThread(
        otherProfileId: String,
        profileId: String,
        limit: Int = 50,
        cursor: String? = null
    ) = apiService.getConversationThread(otherProfileId, profileId, limit, cursor)

    suspend fun sendMessage(
        receiverId: String,
        profileId: String,
        content: String,
        imageIds: List<String>? = null
    ) = apiService.sendMessage(
        profileId = profileId,
        request = SendMessageRequest(
            content = content,
            receiverId = receiverId,
            imageIds = imageIds
        )
    )

    suspend fun markConversationAsRead(
        otherProfileId: String
    ) = apiService.markConversationAsRead(otherProfileId)

    suspend fun markAllConversationsAsRead() =
        apiService.markAllConversationsAsRead()

    suspend fun getUnreadMessagesCount(
        profileId: String
    ) = apiService.getUnreadMessagesCount(profileId)

    // MARK: - Group Chats

    suspend fun getGroupChats(
        profileId: String,
        limit: Int = 20,
        cursor: String? = null
    ) = apiService.getGroupChats(profileId, limit, cursor)

    suspend fun createGroupChat(
        name: String,
        creatorProfileId: String,
        memberProfileIds: List<String>
    ) = apiService.createGroupChat(
        CreateGroupChatRequest(
            name = name,
            creatorProfileId = creatorProfileId,
            memberProfileIds = memberProfileIds
        )
    )

    suspend fun getGroupChat(
        groupChatId: String,
        profileId: String
    ) = apiService.getGroupChat(groupChatId, profileId)

    suspend fun updateGroupChat(
        groupChatId: String,
        profileId: String,
        name: String,
        description: String?
    ) = apiService.updateGroupChat(
        groupChatId,
        profileId,
        UpdateGroupChatRequest(name, description)
    )

    suspend fun deleteGroupChat(
        groupChatId: String,
        profileId: String
    ) = apiService.deleteGroupChat(groupChatId, profileId)

    suspend fun getGroupChatMessages(
        groupChatId: String,
        profileId: String,
        limit: Int = 50,
        cursor: String? = null
    ) = apiService.getGroupChatMessages(groupChatId, profileId, limit, cursor)

    suspend fun sendGroupChatMessage(
        groupChatId: String,
        profileId: String,
        content: String,
        imageIds: List<String>? = null
    ) = apiService.sendGroupChatMessage(
        groupChatId,
        profileId,
        SendGroupMessageRequest(content, imageIds)
    )

    suspend fun addGroupChatMember(
        groupChatId: String,
        profileId: String,
        memberProfileId: String
    ) = apiService.addGroupChatMember(
        groupChatId,
        profileId,
        AddGroupChatMemberRequest(memberProfileId)
    )

    suspend fun removeGroupChatMember(
        groupChatId: String,
        memberProfileId: String,
        profileId: String
    ) = apiService.removeGroupChatMember(groupChatId, memberProfileId, profileId)

    suspend fun leaveGroupChat(
        groupChatId: String,
        profileId: String
    ) = apiService.leaveGroupChat(groupChatId, profileId)

    suspend fun markGroupChatAsRead(
        groupChatId: String,
        profileId: String
    ) = apiService.markGroupChatAsRead(groupChatId, profileId)
}
