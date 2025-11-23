package ng.commu.data.repository

import ng.commu.data.model.Notification
import ng.commu.data.model.NotificationResponse
import ng.commu.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getNotifications(
        profileId: String,
        cursor: String? = null,
        limit: Int = 20
    ): Result<NotificationResponse> {
        return try {
            val response = apiService.getNotifications(profileId, cursor, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.message().takeIf { it.isNotBlank() }
                    ?: response.errorBody()?.string()?.take(100)
                    ?: "Failed to fetch notifications (${response.code()})"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(profileId: String): Result<Int> {
        return try {
            val response = apiService.getUnreadCount(profileId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.count)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch unread count"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(profileId: String): Result<Unit> {
        return try {
            val response = apiService.markAllNotificationsAsRead(profileId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to mark all as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String, profileId: String): Result<Unit> {
        return try {
            val response = apiService.markNotificationAsRead(notificationId, profileId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to mark as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
