package com.wifiguard.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NetworkConnectivityDialog(
    isConnected: Boolean,
    connectionType: com.wifiguard.core.common.ConnectionType,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = if (isConnected) "Подключение восстановлено" else "Нет подключения к интернету",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isConnected) {
                        "Интернет-подключение успешно восстановлено. Приложение теперь может выполнять онлайн-операции."
                    } else {
                        "Для полной функциональности приложения требуется интернет-подключение. " +
                        "Некоторые функции могут быть недоступны до восстановления соединения."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!isConnected) {
                    Text(
                        text = "Советы для восстановления подключения:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• Проверьте Wi-Fi или мобильные данные",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Перезапустите маршрутизатор",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Проверьте режим полета",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Информация о текущем подключении:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Информация о подключении:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Статус: ${getConnectionStatusText(connectionType)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Тип: ${getConnectionTypeText(connectionType)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (isConnected) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "💡 Приложение может выполнять онлайн-операции, такие как обновление базы угроз и отправка аналитики.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isConnected) {
                Button(onClick = onDismiss) {
                    Text("Продолжить")
                }
            } else {
                if (onRetry != null) {
                    Button(onClick = onRetry) {
                        Text("Повторить")
                    }
                } else {
                    Button(onClick = onDismiss) {
                        Text("ОК")
                    }
                }
            }
        },
        dismissButton = {
            if (!isConnected && onRetry != null) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}

@Composable
private fun getConnectionStatusText(connectionType: com.wifiguard.core.common.ConnectionType): String {
    return when (connectionType) {
        com.wifiguard.core.common.ConnectionType.WIFI -> "Подключено к Wi-Fi"
        com.wifiguard.core.common.ConnectionType.CELLULAR -> "Подключено к мобильной сети"
        com.wifiguard.core.common.ConnectionType.ETHERNET -> "Подключено к Ethernet"
        com.wifiguard.core.common.ConnectionType.UNKNOWN -> "Подключено (тип неизвестен)"
        com.wifiguard.core.common.ConnectionType.NONE -> "Нет подключения"
    }
}

@Composable
private fun getConnectionTypeText(connectionType: com.wifiguard.core.common.ConnectionType): String {
    return when (connectionType) {
        com.wifiguard.core.common.ConnectionType.WIFI -> "Wi-Fi"
        com.wifiguard.core.common.ConnectionType.CELLULAR -> "Мобильная сеть"
        com.wifiguard.core.common.ConnectionType.ETHERNET -> "Ethernet"
        com.wifiguard.core.common.ConnectionType.UNKNOWN -> "Неизвестно"
        com.wifiguard.core.common.ConnectionType.NONE -> "Отсутствует"
    }
}