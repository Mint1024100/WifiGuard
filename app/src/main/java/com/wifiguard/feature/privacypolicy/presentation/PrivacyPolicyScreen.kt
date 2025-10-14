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
                text = "Дата вступления в силу: 14 октября 2025 г.",
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
            text = "WifiGuard (" +
                   "приложение, " +
                   "\"мы\", \"наш\", \"нас\") уважает вашу конфиденциальность и " +
                   "обязуется защищать вашу личную информацию. Настоящая политика " +
                   "конфиденциальности описывает, как мы собираем, используем и " +
                   "раскрываем информацию при использовании вами нашего приложения " +
                   "WifiGuard (\"Приложение\").",
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
            text = "Мы используем собранную информацию для:\n\n" +
                   "• Обнаружения потенциальных угроз безопасности Wi-Fi\n" +
                   "• Оценки уровня безопасности Wi-Fi сетей\n" +
                   "• Предоставления пользователям рекомендаций по безопасности\n" +
                   "• Отображения истории сканирования и анализа безопасности",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "5. Передача данных третьим сторонам",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Мы НЕ передаем, не продаем и не предоставляем доступ к " +
                   "вашим данным третьим сторонам. Все данные обрабатываются и " +
                   "хранятся исключительно на вашем локальном устройстве.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "6. Безопасность данных",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Мы внедрили соответствующие технические меры для защиты " +
                   "вашей информации, включая:\n\n" +
                   "• Шифрование чувствительных данных с использованием AES\n" +
                   "• Использование Android Keystore для хранения криптографических ключей\n" +
                   "• Локальное хранение данных без передачи в интернет\n\n" +
                   "Однако ни один метод передачи данных по интернету или метод " +
                   "электронного хранения не является на 100% безопасным, и мы не " +
                   "можем гарантировать его абсолютную безопасность.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "7. Права пользователя",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "У вас есть следующие права в отношении ваших данных:\n\n" +
                   "• Право на доступ к данным, которые мы храним о вас\n" +
                   "• Право на удаление ваших данных из приложения\n" +
                   "• Право на переносимость данных, экспортируя историю сканирования\n\n" +
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
            text = "Мы можем обновить нашу политику конфиденциальности время от " +
                   "времени. Обновленная версия будет указана с новой датой " +
                   "вступления в силу. Мы рекомендуем вам периодически просматривать " +
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
            append("Если у вас есть вопросы или предложения относительно нашей " +
                   "политики конфиденциальности, вы можете связаться с нами по " +
                   "адресу: ")
            
            pushStringAnnotation(
                tag = "email",
                annotation = "mailto:wifiguard@example.com"
            )
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("wifiguard@example.com")
            }
            pop()
        }
        
        ClickableText(
            text = contactAnnotatedString,
            style = MaterialTheme.typography.bodyLarge,
            onClick = { offset ->
                contactAnnotatedString.getStringAnnotations("email", offset, offset)
                    .firstOrNull()?.let { span ->
                        onLinkClick(span.item)
                    }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Дата последнего обновления: 14 октября 2025 г.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}