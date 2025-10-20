package com.wifiguard.feature.about.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.wifiguard.R



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("О приложении") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App icon and name
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "WifiGuard",
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp)
            )
            
            Text(
                text = "WifiGuard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "Версия 1.0",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "WifiGuard — это приложение для анализа безопасности Wi-Fi сетей, обнаружения угроз и мониторинга сетевой активности.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AboutSection(
                    title = "Политика конфиденциальности",
                    content = "Узнайте, как мы собираем, используем и защищаем вашу информацию."
                ) {
                    // This would be handled by navigation
                }
                
                AboutSection(
                    title = "Условия использования",
                    content = "Ознакомьтесь с условиями использования приложения."
                ) {
                    // This would be handled by navigation
                }
                
                AboutSection(
                    title = "Лицензия",
                    content = "Приложение распространяется под лицензией Apache 2.0."
                ) {
                    uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0")
                }
                
                AboutSection(
                    title = "GitHub",
                    content = "Просмотрите исходный код проекта."
                ) {
                    uriHandler.openUri("https://github.com/Mint1024100/wifiguard")
                }
                
                // Contact section
                Text(
                    text = "Контакты",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                val contactAnnotatedString = buildAnnotatedString {
                    append("Для связи с разработчиками: ")
                    
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
                                uriHandler.openUri(span.item)
                            }
                    }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "© 2025 WifiGuard. Все права защищены.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun AboutSection(
    title: String,
    content: String,
    onClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}