package com.wifiguard.feature.termsofservice.presentation

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wifiguard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Условия использования") },
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
                text = "Условия использования WifiGuard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Дата вступления в силу: 14 октября 2025 г.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            TermsOfServiceContent { url ->
                uriHandler.openUri(url)
            }
        }
    }
}

@Composable
fun TermsOfServiceContent(onLinkClick: (String) -> Unit) {
    Column {
        
        Text(
            text = "1. Введение",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Добро пожаловать в WifiGuard! Эти Условия использования " +
                   "управляют вашим использованием мобильного приложения WifiGuard " +
                   "(\"Приложение\"). Используя наше Приложение, вы соглашаетесь " +
                   "соблюдать настоящие Условия и все применимые законы и " +
                   "регулирования.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "2. Использование приложения",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Приложение WifiGuard предназначено исключительно для " +
                   "анализа безопасности Wi-Fi сетей и предоставления " +
                   "рекомендаций по обеспечению безопасности. Вы соглашаетесь " +
                   "использовать Приложение только в законных целях и в " +
                   "соответствии со всеми применимыми законами и регулированиями.\n\n" +
                   "Вы не имеете права:\n" +
                   "• Использовать Приложение для несанкционированного доступа к " +
                   "Wi-Fi сетям или другим сетям\n" +
                   "• Использовать Приложение для незаконной деятельности или " +
                   "вмешательства в работу сетей\n" +
                   "• Пытаться получить доступ к системам или данным, к которым " +
                   "у вас нет разрешения\n" +
                   "• Использовать Приложение в целях, запрещенных законом",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "3. Отказ от гарантий",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Приложение предоставляется \"как есть\", без каких-либо " +
                   "гарантий, явных или подразумеваемых. Мы не гарантируем, что " +
                   "Приложение будет соответствовать всем вашим требованиям или " +
                   "что работа будет бесперебойной или безошибочной.\n\n" +
                   "Мы не делаем никаких гарантий относительно точности, " +
                   "надежности или актуальности информации, предоставляемой " +
                   "Приложением. Результаты анализа безопасности должны " +
                   "рассматриваться как рекомендательные, а не как полная " +
                   "гарантия безопасности.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "4. Ограничение ответственности",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "WifiGuard и его разработчики не несут ответственности за " +
                   "любые убытки, расходы или ущерб, возникшие в результате " +
                   "использования или невозможности использования Приложения.\n\n" +
                   "В максимальной степени, разрешенной действующим законодательством, " +
                   "мы отказываемся от любой ответственности за любые косвенные, " +
                   "случайные, специальные или последующие убытки, включая, но " +
                   "не ограничиваясь, убытки, связанные с потерей прибыли, " +
                   "данных или информации.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "5. Точность анализа",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Приложение WifiGuard стремится предоставить точный анализ " +
                   "безопасности Wi-Fi сетей, но не гарантирует обнаружение всех " +
                   "потенциальных угроз или уязвимостей. Пользователи несут " +
                   "единоличную ответственность за принятие решений о подключении " +
                   "к конкретным Wi-Fi сетям.\n\n" +
                   "Результаты анализа безопасности предоставляются только в " +
                   "информационных целях и не должны использоваться как единственный " +
                   "фактор при принятии решения о сетевой безопасности.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "6. Сторонние сервисы",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Приложение не использует сторонние сервисы для анализа " +
                   "данных или предоставления функций. Все аналитические функции " +
                   "работают локально на вашем устройстве без передачи данных " +
                   "третьим сторонам.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "7. Изменения условий",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Мы оставляем за собой право изменять настоящие Условия " +
                   "использования в любое время. Любые изменения будут публиковаться " +
                   "в Приложении с новой датой вступления в силу. Продолжение " +
                   "использования Приложения после внесения изменений означает " +
                   "ваше согласие с обновленными Условиями.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "8. Применимое право",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Настоящие Условия подлежат регулированию и толкованию в " +
                   "соответствии с законодательством страны, в которой вы " +
                   "находитесь, если иное не предусмотрено применимым " +
                   "международным соглашением.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "9. Контактная информация",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        val contactAnnotatedString = buildAnnotatedString {
            append("Если у вас есть вопросы или предложения относительно " +
                   "настоящих Условий использования, вы можете связаться с нами по " +
                   "адресу: ")

            pushStringAnnotation(tag = LINK_TAG, annotation = "mailto:wifiguard@example.com")
            withStyle(
                SpanStyle(
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
            text = "Дата последнего обновления: 14 октября 2025 г.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private const val LINK_TAG = "URL"