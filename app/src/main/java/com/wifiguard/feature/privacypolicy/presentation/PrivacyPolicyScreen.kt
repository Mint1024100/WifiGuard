package com.wifiguard.feature.privacypolicy.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wifiguard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Политика конфиденциальности") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Политика конфиденциальности WifiGuard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Дата вступления в силу: 12 декабря 2025 г.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            PrivacyPolicyContent { url ->
                uriHandler.openUri(url)
            }
        }
    }
}

@Composable
fun PrivacyPolicyContent(onLinkClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        Text(
            text = "1. Введение",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "WifiGuard — это Android-приложение для анализа безопасности Wi-Fi сетей. " +
                   "Настоящая политика конфиденциальности описывает, как приложение собирает, " +
                   "использует и защищает информацию при его использовании.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "2. Сбор и использование информации",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Для предоставления функций безопасности Wi-Fi и анализа " +
                   "угроз, мы собираем следующие категории информации:",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Text(
            text = "\n• Информация о Wi-Fi сетях: SSID, BSSID, уровень сигнала, тип " +
                   "шифрования, частота и канал. Эти данные используются исключительно " +
                   "для анализа безопасности сетей и не содержат вашей личной информации.\n\n" +
                   "• Данные о местоположении: Приложение запрашивает разрешение на " +
                   "доступ к местоположению исключительно для функции обнаружения Wi-Fi " +
                   "сетей, что требует Android 6.0 и выше для сканирования сетей. " +
                   "Данные о местоположении не передаются на наши серверы и хранятся " +
                   "только локально на вашем устройстве.\n\n" +
                   "• История сканирования: Приложение сохраняет историю сканирования " +
                   "в локальной базе данных на вашем устройстве. Эти данные используются " +
                   "для анализа тенденций и угроз и не передаются третьим сторонам.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "3. Технологии слежения",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Приложение WifiGuard НЕ использует:\n\n" +
                   "• Технологии аналитики (Google Analytics, Firebase Analytics и т.д.)\n" +
                   "• Сторонние SDK для рекламы\n" +
                   "• Технологии отслеживания пользовательского поведения\n\n" +
                   "Все данные обрабатываются локально на вашем устройстве и не " +
                   "передаются на наши или сторонние серверы.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "4. Использование информации",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Приложение использует собранную информацию для:\n\n" +
                   "• Обнаружения потенциальных угроз безопасности Wi-Fi\n" +
                   "• Оценки уровня безопасности Wi-Fi сетей\n" +
                   "• Предоставления пользователям рекомендаций по безопасности\n" +
                   "• Отображения истории сканирования и анализа безопасности\n" +
                   "• Фонового мониторинга сетевого окружения (если включено)",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "5. Передача данных третьим сторонам",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Приложение НЕ передает, не продает и не предоставляет доступ к " +
                   "вашим данным третьим сторонам. Все данные обрабатываются и " +
                   "хранятся исключительно на вашем локальном устройстве.\n\n" +
                   "• Никакие данные НЕ отправляются на внешние серверы\n" +
                   "• Приложение работает полностью офлайн\n" +
                   "• НЕ используются аналитические сервисы\n" +
                   "• НЕ показывается реклама",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "6. Безопасность данных",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Приложение использует следующие технические меры для защиты " +
                   "вашей информации:\n\n" +
                   "• Шифрование данных с использованием AES-256\n" +
                   "• Использование Android Keystore для хранения ключей шифрования\n" +
                   "• Локальное хранение данных без передачи в интернет\n" +
                   "• Безопасное хранение настроек (DataStore Preferences)\n\n" +
                   "Поскольку данные не передаются через интернет, риски " +
                   "перехвата данных минимальны.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "7. Права пользователя",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "У вас есть следующие права в отношении ваших данных:\n\n" +
                   "• Удалить данные: Очистить историю сканирований в настройках\n" +
                   "• Отозвать разрешения: Отключить любые разрешения в настройках Android\n" +
                   "• Удалить приложение: При удалении все данные автоматически удаляются\n" +
                   "• Отключить фоновый мониторинг: Можно отключить в настройках приложения\n\n" +
                   "Вы можете реализовать эти права, используя функции настроек " +
                   "приложения или удалив само приложение.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "8. Изменения в политике конфиденциальности",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Разработчик может обновить политику конфиденциальности время от " +
                   "времени. Обновленная версия будет указана с новой датой " +
                   "вступления в силу. Рекомендуется периодически просматривать " +
                   "настоящую политику конфиденциальности на предмет изменений. " +
                   "Продолжение использования приложения после внесения изменений " +
                   "означает принятие обновленной политики.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "9. Контактная информация",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        val contactAnnotatedString = buildAnnotatedString {
            append("Полная версия политики конфиденциальности доступна по адресу: ")

            pushStringAnnotation(tag = LINK_TAG, annotation = "https://mint1024100.github.io/WifiGuard/privacy_policy.html")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("https://mint1024100.github.io/WifiGuard/privacy_policy.html")
            }
            pop()
            
            append("\n\nEmail: ")

            pushStringAnnotation(tag = LINK_TAG, annotation = "mailto:svatozarbozylev@gmail.com")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("svatozarbozylev@gmail.com")
            }
            pop()
            
            append("\nGitHub: ")
            
            pushStringAnnotation(tag = LINK_TAG, annotation = "https://github.com/Mint1024100/WifiGuard")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("https://github.com/Mint1024100/WifiGuard")
            }
            pop()
        }

        ClickableText(
            text = contactAnnotatedString,
            style = MaterialTheme.typography.bodyLarge,
            onClick = { offset ->
                contactAnnotatedString.getStringAnnotations(
                    tag = LINK_TAG,
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Дата последнего обновления: 12 декабря 2025 г.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private const val LINK_TAG = "URL"