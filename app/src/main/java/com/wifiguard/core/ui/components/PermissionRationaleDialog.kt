package com.wifiguard.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wifiguard.R

@Composable
fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOpenSettings: () -> Unit,
    isPermanentlyDenied: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = if (isPermanentlyDenied) {
                    "Требуется разрешение"
                } else {
                    "Зачем нужен доступ к местоположению?"
                }
            )
        },
        text = {
            Text(
                text = if (isPermanentlyDenied) {
                    "Для сканирования Wi-Fi сетей требуется разрешение на доступ к местоположению. " +
                    "Это требование Android, а не нашего приложения.\n\n" +
                    "Пожалуйста, предоставьте разрешение в настройках приложения.\n\n" +
                    "Примечание: Мы НЕ отслеживаем Ваше местоположение и НЕ используем GPS."
                } else {
                    "Для сканирования Wi-Fi сетей Android требует разрешение на доступ к местоположению.\n\n" +
                    "Это требование операционной системы (начиная с Android 6.0), а не нашего приложения.\n\n" +
                    "⚠️ Важно: Мы НЕ отслеживаем ваше местоположение! Разрешение используется ТОЛЬКО для получения списка Wi-Fi сетей."
                }
            )
        },
        confirmButton = {
            Button(onClick = if (isPermanentlyDenied) onOpenSettings else onConfirm) {
                Text(if (isPermanentlyDenied) "Открыть настройки" else "Предоставить разрешение")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}