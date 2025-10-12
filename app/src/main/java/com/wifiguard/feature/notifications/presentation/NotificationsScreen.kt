package com.wifiguard.feature.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifiguard.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран уведомлений
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String) -> Unit
) {
    // Mock notifications for demonstration
    val notifications = remember {
        listOf(
            NotificationItem(
                id = "1",
                type = NotificationType.WARNING,
                title = "Обнаружена подозрительная сеть",
                description = "Сеть 'Free_WiFi' может быть небезопасной",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 5, // 5 minutes ago
                networkId = "Free_WiFi",
                isRead = false
            ),
            NotificationItem(
                id = "2",
                type = NotificationType.ERROR,
                title = "Открытая сеть без шифрования",
                description = "Сеть 'Guest_Network' не использует шифрование",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 30, // 30 minutes ago
                networkId = "Guest_Network",
                isRead = false
            ),
            NotificationItem(
                id = "3",
                type = NotificationType.INFO,
                title = "Сканирование завершено",
                description = "Найдено 12 Wi-Fi сетей",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60, // 1 hour ago
                networkId = null,
                isRead = true
            ),
            NotificationItem(
                id = "4",
                type = NotificationType.WARNING,
                title = "Устаревшее шифрование WEP",
                description = "Сеть 'Legacy_Network' использует устаревший протокол",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2, // 2 hours ago
                networkId = "Legacy_Network",
                isRead = true
            ),
            NotificationItem(
                id = "5",
                type = NotificationType.INFO,
                title = "Фоновое сканирование",
                description = "Обнаружено 3 новые сети",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24, // Yesterday
                networkId = null,
                isRead = true
            )
        )
    }
    
    val groupedNotifications = remember(notifications) {
        notifications.groupBy { notification ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = notification.timestamp
            
            val now = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            
            when {
                calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> "Сегодня"
                calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) &&
                calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) -> "Вчера"
                else -> "Ранее"
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Уведомления") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Mark all as read */ }) {
                        Icon(Icons.Default.DoneAll, "Отметить все как прочитанные")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Нет уведомлений",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Здесь будут отображаться уведомления о безопасности",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedNotifications.forEach { (dateGroup, notificationsInGroup) ->
                    item {
                        Text(
                            text = dateGroup,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(
                        items = notificationsInGroup,
                        key = { it.id }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                notification.networkId?.let { networkId ->
                                    onNavigateToAnalysis(networkId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            NotificationType.ERROR -> DangerRed
                            NotificationType.WARNING -> WarningOrange
                            NotificationType.INFO -> MaterialTheme.colorScheme.primary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        NotificationType.ERROR -> Icons.Default.Error
                        NotificationType.WARNING -> Icons.Default.Warning
                        NotificationType.INFO -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                
                Text(
                    text = notification.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (notification.networkId != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Только что"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} мин. назад"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} ч. назад"
        else -> {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("ru"))
            sdf.format(Date(timestamp))
        }
    }
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val description: String,
    val timestamp: Long,
    val networkId: String?,
    val isRead: Boolean
)

enum class NotificationType {
    ERROR,
    WARNING,
    INFO
}


