package com.wifiguard.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана уведомлений
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    // TODO: Inject notification repository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    
    init {
        loadNotifications()
    }
    
    private fun loadNotifications() {
        viewModelScope.launch {
            // TODO: Load notifications from repository
            val mockNotifications = listOf(
                NotificationItem(
                    id = "1",
                    title = "Обнаружена открытая сеть",
                    message = "Сеть 'FreeWiFi' не имеет шифрования",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 30, // 30 minutes ago
                    isRead = false
                ),
                NotificationItem(
                    id = "2",
                    title = "Подозрительная активность",
                    message = "Обнаружены дублирующиеся SSID",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2, // 2 hours ago
                    isRead = true
                )
            )
            
            _uiState.value = _uiState.value.copy(notifications = mockNotifications)
        }
    }
    
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val updatedNotifications = _uiState.value.notifications.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
            
            _uiState.value = _uiState.value.copy(notifications = updatedNotifications)
            // TODO: Update in repository
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            val updatedNotifications = _uiState.value.notifications.map { notification ->
                notification.copy(isRead = true)
            }
            
            _uiState.value = _uiState.value.copy(notifications = updatedNotifications)
            // TODO: Update in repository
        }
    }
}

/**
 * Состояние UI экрана уведомлений
 */
data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Элемент уведомления
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
