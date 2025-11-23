package ng.commu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.Notification
import ng.commu.data.repository.NotificationRepository
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notificationsState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val notificationsState: StateFlow<NotificationsUiState> = _notificationsState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var currentCursor: String? = null
    private var hasMore: Boolean = false
    private var currentProfileId: String? = null

    fun loadNotifications(profileId: String?, refresh: Boolean = false) {
        if (profileId == null) {
            _notificationsState.value = NotificationsUiState.Error("No profile selected")
            return
        }

        if (refresh) {
            currentCursor = null
            hasMore = false
            currentProfileId = profileId
        }

        // If not refreshing and already loading or no more items, skip
        if (!refresh && (_notificationsState.value is NotificationsUiState.Loading || !hasMore && currentCursor != null)) {
            return
        }

        viewModelScope.launch {
            if (refresh) {
                _notificationsState.value = NotificationsUiState.Loading
            } else {
                _notificationsState.value = NotificationsUiState.LoadingMore(
                    (_notificationsState.value as? NotificationsUiState.Success)?.notifications ?: emptyList()
                )
            }

            notificationRepository.getNotifications(profileId, currentCursor)
                .onSuccess { response ->
                    val existingNotifications = if (refresh) {
                        emptyList()
                    } else {
                        (_notificationsState.value as? NotificationsUiState.Success)?.notifications
                            ?: (_notificationsState.value as? NotificationsUiState.LoadingMore)?.notifications
                            ?: emptyList()
                    }

                    currentCursor = response.pagination.nextCursor
                    hasMore = response.pagination.hasMore

                    _notificationsState.value = NotificationsUiState.Success(
                        notifications = existingNotifications + response.data
                    )
                }
                .onFailure { error ->
                    _notificationsState.value = NotificationsUiState.Error(
                        error.message ?: "Failed to load notifications"
                    )
                }
        }
    }

    fun loadUnreadCount(profileId: String?) {
        if (profileId == null) return

        viewModelScope.launch {
            notificationRepository.getUnreadCount(profileId)
                .onSuccess { count ->
                    _unreadCount.value = count
                }
                .onFailure {
                    // Silently fail - unread count is not critical
                }
        }
    }

    fun markAsRead(notificationId: String) {
        val profileId = currentProfileId ?: return

        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId, profileId)
                .onSuccess {
                    // Update the notification in the list
                    val currentState = _notificationsState.value
                    if (currentState is NotificationsUiState.Success) {
                        val updatedNotifications = currentState.notifications.map { notification ->
                            if (notification.id == notificationId) {
                                notification.copy(readAt = "read")
                            } else {
                                notification
                            }
                        }
                        _notificationsState.value = NotificationsUiState.Success(updatedNotifications)
                    }

                    // Update unread count
                    if (_unreadCount.value > 0) {
                        _unreadCount.value = _unreadCount.value - 1
                    }
                }
                .onFailure {
                    // Silently fail - marking as read is not critical
                }
        }
    }

    fun markAllAsRead() {
        val profileId = currentProfileId ?: return

        viewModelScope.launch {
            notificationRepository.markAllAsRead(profileId)
                .onSuccess {
                    // Update all notifications in the list
                    val currentState = _notificationsState.value
                    if (currentState is NotificationsUiState.Success) {
                        val updatedNotifications = currentState.notifications.map { notification ->
                            notification.copy(readAt = "read")
                        }
                        _notificationsState.value = NotificationsUiState.Success(updatedNotifications)
                    }

                    // Reset unread count
                    _unreadCount.value = 0
                }
                .onFailure {
                    // Silently fail
                }
        }
    }
}

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class LoadingMore(val notifications: List<Notification>) : NotificationsUiState()
    data class Success(val notifications: List<Notification>) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}
