package com.wifiguard.feature.analysis.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.security.SecurityReport
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import android.os.Build
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import com.wifiguard.core.ui.theme.calculateLuminance
import com.wifiguard.core.data.wifi.ScanStatusState
import com.wifiguard.core.domain.model.WifiScanStatus
import com.wifiguard.core.service.WifiForegroundScanService

/**
 * Экран анализа безопасности
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSecurityReport: () -> Unit = {},
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // ИСПРАВЛЕНО: Получаем context напрямую (LocalContext.current безопасен в @Composable)
    val context = LocalContext.current

    // ИСПРАВЛЕНО: Разделены два LaunchedEffect для предотвращения циклов
    // Первый: однократный запуск при входе на экран
    LaunchedEffect(Unit) {
        // Первоначальный запрос автоскана
        viewModel.requestAutoScan()
        
        // Запускаем сканирование, если данных нет или они устарели
        val hasData = uiState.securityReport != null
        val dataAge = if (uiState.lastUpdateTime > 0) {
            System.currentTimeMillis() - uiState.lastUpdateTime
        } else {
            Long.MAX_VALUE
        }
        
        if (!hasData || dataAge > 300_000L) {
            try {
                WifiForegroundScanService.start(context)
            } catch (e: Exception) {
                Log.e("AnalysisScreen", "Ошибка при запуске сканирования", e)
            }
        }
    }
    
    // Второй: периодическое обновление (не зависит от lastUpdateTime, чтобы избежать циклов)
    LaunchedEffect(Unit) {
        // Периодическое обновление данных каждые 5 минут
        // ИСПРАВЛЕНО: Используем try-catch для обработки CancellationException
        try {
            while (true) {
                kotlinx.coroutines.delay(300_000L) // 5 минут
                
                // Проверяем актуальное состояние перед запуском сканирования
                // Используем текущее состояние из uiState, но не делаем его зависимостью
                val currentState = viewModel.uiState.value
                val currentDataAge = if (currentState.lastUpdateTime > 0) {
                    System.currentTimeMillis() - currentState.lastUpdateTime
                } else {
                    Long.MAX_VALUE
                }
                
                // Запускаем сканирование если данные старше 5 минут
                if (currentDataAge > 300_000L) {
                    try {
                        WifiForegroundScanService.start(context)
                    } catch (e: Exception) {
                        Log.e("AnalysisScreen", "Ошибка при периодическом сканировании", e)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Корректная отмена корутины при уничтожении компонента
            throw e
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Анализ безопасности",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight(600)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                            viewModel.refreshData(context)
                        } catch (e: Exception) {
                            Log.e("AnalysisScreen", "Ошибка при обновлении через кнопку", e)
                        }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить сканирование",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp) // Consistent 16dp padding around all screen edges
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ScanStatusBanner(
                scanStatus = uiState.scanStatus,
                securityReport = uiState.securityReport,
                lastUpdateTime = uiState.lastUpdateTime
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ошибка загрузки данных",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                try {
                                viewModel.refreshData(context)
                            } catch (e: Exception) {
                                Log.e("AnalysisScreen", "Ошибка при повторе сканирования", e)
                            }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить сканирование")
                        }
                    }
                }
            } else {
                // ИСПРАВЛЕНО: Сохраняем securityReport в локальную переменную для smart cast
                val securityReport = uiState.securityReport
                if (securityReport != null) {
                    // ИСПРАВЛЕНО: Используем derivedStateOf с правильными ключами для оптимизации
                    val isRefreshing = remember(uiState.scanStatus) {
                        derivedStateOf {
                            uiState.scanStatus is ScanStatusState.Scanning || 
                            uiState.scanStatus is ScanStatusState.Starting ||
                            uiState.scanStatus is ScanStatusState.Processing
                        }
                    }.value
                    
                    // #region agent log
                    LaunchedEffect(isRefreshing, uiState.scanStatus) {
                        try {
                            val logFile = java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log")
                            val logEntry = org.json.JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "D")
                                put("location", "AnalysisScreen.kt:PullToRefreshBox")
                                put("message", "isRefreshing или scanStatus изменилось")
                                put("timestamp", System.currentTimeMillis())
                                put("data", org.json.JSONObject().apply {
                                    put("isRefreshing", isRefreshing)
                                    put("scanStatus", uiState.scanStatus.javaClass.simpleName)
                                })
                            }
                            logFile.appendText(logEntry.toString() + "\n")
                        } catch (e: Exception) { /* ignore */ }
                    }
                    // #endregion
                    
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            // #region agent log
                            try {
                                val logFile = java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log")
                                val logEntry = org.json.JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "run1")
                                    put("hypothesisId", "E")
                                    put("location", "AnalysisScreen.kt:onRefresh")
                                    put("message", "onRefresh вызван")
                                    put("timestamp", System.currentTimeMillis())
                                    put("data", org.json.JSONObject().apply {
                                        put("isRefreshing", isRefreshing)
                                        put("scanStatus", uiState.scanStatus.javaClass.simpleName)
                                    })
                                }
                                logFile.appendText(logEntry.toString() + "\n")
                            } catch (e: Exception) { /* ignore */ }
                            // #endregion
                            try {
                                viewModel.refreshData(context)
                            } catch (e: Exception) {
                                // Ошибка уже обработана в ViewModel, здесь только логируем
                                Log.e("AnalysisScreen", "Ошибка при обновлении данных", e)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SecurityReportContent(
                            report = securityReport,
                            onNavigateToFullReport = onNavigateToSecurityReport
                        )
                    }
                } else {
                    // Состояние "нет данных" - показываем пустой экран
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Нет данных для анализа",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Выполните сканирование для получения анализа",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        viewModel.refreshData(context)
                                    } catch (e: Exception) {
                                        Log.e("AnalysisScreen", "Ошибка при обновлении сканирования", e)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Обновить сканирование")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanStatusBanner(
    scanStatus: ScanStatusState,
    securityReport: com.wifiguard.core.security.SecurityReport?,
    lastUpdateTime: Long
) {
    // ИСПРАВЛЕНО: Баннер всегда показывается с актуальными данными из БД
    // Используем данные из securityReport для отображения статуса
    
    // Форматируем время последнего обновления
    val timeAgo = if (lastUpdateTime > 0) {
        val secondsAgo = (System.currentTimeMillis() - lastUpdateTime) / 1000
        when {
            secondsAgo < 60 -> "только что"
            secondsAgo < 3600 -> "${secondsAgo / 60} мин. назад"
            secondsAgo < 86400 -> "${secondsAgo / 3600} ч. назад"
            else -> "${secondsAgo / 86400} дн. назад"
        }
    } else {
        "никогда"
    }
    
    // Определяем заголовок и подзаголовок на основе состояния сканирования и данных
    val (title, subtitle, isLoading, isError) = when (scanStatus) {
        is ScanStatusState.Starting -> Quad("Запуск сканирования…", "Подготовка", true, false)
        is ScanStatusState.Scanning -> Quad("Сканирование…", "Поиск Wi‑Fi сетей", true, false)
        is ScanStatusState.Processing -> Quad(
            "Обработка…",
            scanStatus.networksCount?.let { "Найдено сетей: $it" } ?: "Обновление данных",
            true,
            false
        )
        is ScanStatusState.Completed -> Quad(
            "Данные обновлены",
            "Сетей: ${scanStatus.networksCount}, угроз: ${scanStatus.threatsCount}",
            false,
            false
        )
        is ScanStatusState.Result -> {
            when (val status = scanStatus.status) {
                is WifiScanStatus.Throttled -> {
                    // Показываем актуальные данные из БД даже при throttling
                    val networksCount = securityReport?.totalNetworks ?: 0
                    val threatsCount = securityReport?.threats?.size ?: 0
                    val seconds = ((status.nextAvailableTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                    Quad(
                        "Данные обновлены",
                        "Сетей: $networksCount, угроз: $threatsCount • Обновлено $timeAgo • Следующее сканирование через ~${seconds / 60} мин.",
                        false,
                        false
                    )
                }
                is WifiScanStatus.Restricted -> {
                    // Показываем актуальные данные из БД даже при ограничениях
                    val networksCount = securityReport?.totalNetworks ?: 0
                    val threatsCount = securityReport?.threats?.size ?: 0
                    Quad(
                        "Данные обновлены",
                        "Сетей: $networksCount, угроз: $threatsCount • Обновлено $timeAgo • Новое сканирование временно недоступно",
                        false,
                        false
                    )
                }
                is WifiScanStatus.Failed -> {
                    // При ошибке показываем последние доступные данные
                    val networksCount = securityReport?.totalNetworks ?: 0
                    val threatsCount = securityReport?.threats?.size ?: 0
                    Quad(
                        "Ошибка сканирования",
                        if (networksCount > 0) {
                            "Сетей: $networksCount, угроз: $threatsCount • Обновлено $timeAgo • ${status.error}"
                        } else {
                            status.error
                        },
                        false,
                        true
                    )
                }
                is WifiScanStatus.Success -> Quad("Сканирование выполнено", "Данные обновляются…", true, false)
            }
        }
        ScanStatusState.Idle -> {
            // В состоянии Idle показываем данные из БД
            val networksCount = securityReport?.totalNetworks ?: 0
            val threatsCount = securityReport?.threats?.size ?: 0
            if (networksCount > 0) {
                Quad(
                    "Данные обновлены",
                    "Сетей: $networksCount, угроз: $threatsCount • Обновлено $timeAgo",
                    false,
                    false
                )
            } else {
                Quad("Ожидание данных", "Выполняется первое сканирование…", true, false)
            }
        }
    }

    val container = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        scanStatus is ScanStatusState.Completed -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        scanStatus is ScanStatusState.Completed -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
            if (isLoading) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Вспомогательная структура для компактного when-mapping.
 */
private data class Quad(
    val title: String,
    val subtitle: String,
    val isLoading: Boolean,
    val isError: Boolean
)

@Composable
fun SecurityReportContent(
    report: SecurityReport,
    onNavigateToFullReport: () -> Unit = {}
) {
    // ИСПРАВЛЕНО: Получаем конфигурацию один раз и кэшируем вычисленные значения
    val configuration = LocalConfiguration.current
    val screenWidthDp = remember(configuration.screenWidthDp) { configuration.screenWidthDp }
    val isSmallScreen = remember(screenWidthDp) { screenWidthDp < 600 } // Маленькие экраны (телефоны)
    val isMediumScreen = remember(screenWidthDp) { screenWidthDp in 600..840 } // Средние экраны (планшеты)
    
    // ИСПРАВЛЕНО: Валидация данных SecurityReport перед использованием
    val validatedReport = remember(
        report.totalNetworks,
        report.safeNetworks,
        report.lowRiskNetworks,
        report.mediumRiskNetworks,
        report.highRiskNetworks,
        report.criticalRiskNetworks,
        report.threats.size,
        report.recommendations.size,
        report.overallRiskLevel,
        report.timestamp
    ) {
        // Валидируем и нормализуем данные отчета
        val totalNetworks = report.totalNetworks.coerceAtLeast(0)
        report.copy(
            totalNetworks = totalNetworks,
            safeNetworks = report.safeNetworks.coerceIn(0, totalNetworks),
            lowRiskNetworks = report.lowRiskNetworks.coerceIn(0, totalNetworks),
            mediumRiskNetworks = report.mediumRiskNetworks.coerceIn(0, totalNetworks),
            highRiskNetworks = report.highRiskNetworks.coerceIn(0, totalNetworks),
            criticalRiskNetworks = report.criticalRiskNetworks.coerceIn(0, totalNetworks),
            threats = report.threats.filterNotNull().filter { it.id >= 0 },
            recommendations = report.recommendations.filterNotNull().filter { it.isNotBlank() },
            networkAnalysis = report.networkAnalysis.filterNotNull()
        )
    }
    
    // ИСПРАВЛЕНО: Валидация securityScore (диапазон 0-100)
    val rawSecurityScore = validatedReport.getOverallSecurityScoreDetailed()
    val securityScore = rawSecurityScore.coerceIn(0, 100)
    var animatedScore by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(securityScore) {
        animatedScore = securityScore.toFloat()
    }

    var threatsExpanded by remember { mutableStateOf(true) }
    var recommendationsExpanded by remember { mutableStateOf(true) }

    // ИСПРАВЛЕНО: Адаптивный размер колонок в зависимости от размера экрана
    // Добавлены дополнительные breakpoints для лучшей адаптивности
    val gridMinSize = remember(screenWidthDp) {
        when {
            screenWidthDp < 360 -> 260.dp // Очень маленькие экраны
            screenWidthDp < 600 -> 280.dp // Маленькие экраны (телефоны)
            screenWidthDp < 840 -> 320.dp // Средние экраны (планшеты)
            screenWidthDp < 1200 -> 340.dp // Большие планшеты
            else -> 400.dp // Очень большие экраны
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = gridMinSize),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section with Animated Security Score
        item(span = { GridItemSpan(maxLineSpan) }) {
            SecurityScoreHeroSection(
                securityScore = animatedScore,
                overallRiskLevel = validatedReport.overallRiskLevel
            )
        }

        // Stats Cards Grid
        item(span = { GridItemSpan(maxLineSpan) }) {
            StatsCardsGrid(
                report = validatedReport
            )
        }

        // Threats Section
        if (validatedReport.threats.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ThreatsSection(
                    title = "Обнаруженные угрозы",
                    icon = Icons.Rounded.Warning,
                    threats = validatedReport.threats,
                    expanded = threatsExpanded,
                    onToggle = { threatsExpanded = !threatsExpanded },
                    onNavigateToFullReport = onNavigateToFullReport
                )
            }
        }

        // Recommendations Section
        if (validatedReport.recommendations.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RecommendationsSection(
                    title = "Рекомендации",
                    icon = Icons.Rounded.Lightbulb,
                    recommendations = validatedReport.recommendations,
                    expanded = recommendationsExpanded,
                    onToggle = { recommendationsExpanded = !recommendationsExpanded }
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
        }
    }
}

@Composable
fun CollapsibleSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Свернуть" else "Развернуть",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SecurityScoreHeroSection(
    securityScore: Float,
    overallRiskLevel: ThreatLevel
) {
    // ИСПРАВЛЕНО: Получаем конфигурацию один раз и кэшируем вычисленные значения
    val configuration = LocalConfiguration.current
    val screenWidthDp = remember(configuration.screenWidthDp) { configuration.screenWidthDp }
    val isSmallScreen = remember(screenWidthDp) { screenWidthDp < 600 }
    
    // Адаптивные размеры для кругового индикатора
    val circleSize = if (isSmallScreen) 160.dp else 200.dp
    val scoreFontSize = if (isSmallScreen) 28.sp else 36.sp
    val labelFontSize = if (isSmallScreen) 12.sp else 14.sp
    val statusFontSize = if (isSmallScreen) 14.sp else 16.sp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // 8dp elevation
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp) // Consistent card corner radius: 16dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isSmallScreen) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Общая оценка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val animatedProgress by animateFloatAsState(
                targetValue = securityScore / 100f,
                animationSpec = tween(durationMillis = 1000),
                label = "ProgressAnimation"
            )

            val primaryColor = when {
                securityScore >= 90 -> Color(0xFF4CAF50) // Safe Green
                securityScore >= 70 -> Color(0xFF8BC34A) // Low Risk Green
                securityScore >= 50 -> Color(0xFFFFC107) // Medium Amber
                securityScore >= 30 -> Color(0xFFFF9800) // High Orange
                else -> Color(0xFFF44336) // Critical Red
            }
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(circleSize)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw Track
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16f)
                    )

                    // Draw Progress
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360 * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 16f, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${securityScore.toInt()}/100",
                        fontSize = scoreFontSize,  // ИСПРАВЛЕНО: Адаптивный размер шрифта
                        fontWeight = FontWeight(700),  // fontWeight=700
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,  // Prevent wrapping
                        overflow = TextOverflow.Visible, // Prevent cutting off (ellipsize="none" equivalent)
                        modifier = Modifier.align(Alignment.CenterHorizontally) // Center horizontally in parent
                    )
                    Text(
                        text = when (overallRiskLevel) {
                            ThreatLevel.SAFE, ThreatLevel.LOW -> "БЕЗОПАСНО"
                            ThreatLevel.MEDIUM -> "СРЕДНИЙ РИСК"
                            ThreatLevel.HIGH -> "ВЫСОКИЙ РИСК"
                            ThreatLevel.CRITICAL -> "КРИТИЧЕСКИЙ"
                            else -> "АНАЛИЗ"
                        },
                        fontSize = labelFontSize,  // ИСПРАВЛЕНО: Адаптивный размер шрифта
                        fontWeight = FontWeight(500),  // fontWeight=500
                        color = primaryColor,
                        maxLines = 1  // Prevent wrapping
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val statusText = when (overallRiskLevel) {
                ThreatLevel.SAFE, ThreatLevel.LOW -> "Система в безопасности"
                ThreatLevel.MEDIUM -> "Требуется внимание"
                ThreatLevel.HIGH, ThreatLevel.CRITICAL -> "Нужны действия"
                else -> "Анализ"
            }

            val statusIcon = when (overallRiskLevel) {
                ThreatLevel.SAFE, ThreatLevel.LOW -> Icons.Rounded.Shield
                ThreatLevel.MEDIUM -> Icons.Rounded.Warning
                ThreatLevel.HIGH, ThreatLevel.CRITICAL -> Icons.Rounded.Report
                else -> Icons.Rounded.Info
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val statusColor = when (overallRiskLevel) {
                    ThreatLevel.SAFE, ThreatLevel.LOW -> Color(0xFF4CAF50)
                    ThreatLevel.MEDIUM -> Color(0xFFFFC107) // Yellow for medium
                    ThreatLevel.HIGH, ThreatLevel.CRITICAL -> Color(0xFFF44336) // Red for high/critical
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusText,
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    fontSize = statusFontSize,  // ИСПРАВЛЕНО: Адаптивный размер шрифта
                    fontWeight = FontWeight(500),  // fontWeight=500
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun StatsCardsGrid(
    report: SecurityReport
) {
    Spacer(modifier = Modifier.height(24.dp)) // 24dp margin between "Общая оценка" card and statistics section
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // 8dp elevation
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant, // Theme-adaptive background
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp) // Consistent card corner radius: 16dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // First row of stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Equal 12dp spacing between statistics cards
            ) {
                StatsCard(
                    title = "Всего",
                    value = report.totalNetworks,
                    icon = Icons.Rounded.Wifi,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                StatsCard(
                    title = "Безопасные",
                    value = report.safeNetworks,
                    icon = Icons.Rounded.Security,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                StatsCard(
                    title = "Низкий",
                    value = report.lowRiskNetworks,
                    icon = Icons.Rounded.CheckCircle,
                    color = Color(0xFF8BC34A),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp)) // Equal 12dp spacing between statistics cards

            // Second row of stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Equal 12dp spacing between statistics cards
            ) {
                StatsCard(
                    title = "Средний",
                    value = report.mediumRiskNetworks,
                    icon = Icons.Rounded.Warning,
                    color = Color(0xFFFFC107), // Match badge color: yellow
                    modifier = Modifier.weight(1f)
                )

                StatsCard(
                    title = "Высокий",
                    value = report.highRiskNetworks,
                    icon = Icons.Rounded.Report,
                    color = Color(0xFFFF9800), // Match badge color: orange
                    modifier = Modifier.weight(1f)
                )

                StatsCard(
                    title = "Критичный",
                    value = report.criticalRiskNetworks,
                    icon = Icons.Rounded.Error,
                    color = Color(0xFFF44336), // Match badge color: red
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // 8dp elevation to statistics cards
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface, // Theme-adaptive background
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp) // Consistent card corner radius: 16dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // Updated to 12dp padding inside each statistics card
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title, // Add contentDescription for accessibility
                tint = color,
                modifier = Modifier.size(32.dp) // Standardize all icons in statistics cards to 32dp size
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value.toString(),
                fontSize = 24.sp, // 24sp, fontWeight=700 for statistics numbers
                fontWeight = FontWeight(700),
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp, // 12sp, fontWeight=400 for statistics labels
                fontWeight = FontWeight(400),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThreatsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    threats: List<com.wifiguard.core.domain.model.SecurityThreat>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onNavigateToFullReport: () -> Unit = {}
) {
    // ИСПРАВЛЕНО: Получаем конфигурацию один раз и кэшируем вычисленные значения
    val configuration = LocalConfiguration.current
    val screenHeightDp = remember(configuration.screenHeightDp) { configuration.screenHeightDp }
    val isSmallScreen = remember(screenHeightDp) { screenHeightDp < 800 }
    
    // ИСПРАВЛЕНО: Состояние для разворачивания всех угроз
    var showAllThreats by remember { mutableStateOf(false) }
    
    // ИСПРАВЛЕНО: Обработка пустого списка угроз
    if (threats.isEmpty()) {
        return
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок секции
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Список угроз (прозрачные элементы внутри секции)
            if (expanded) {
                // ИСПРАВЛЕНО: Оптимизированная сортировка с более стабильным ключом
                // Используем хеш-код списка на основе первых ID и размера для стабильности remember
                val threatsHash = threats.map { it.id }.take(10).hashCode() to threats.size
                val sortedThreats = remember(threatsHash) {
                    threats.sortedWith(
                        compareByDescending<com.wifiguard.core.domain.model.SecurityThreat> { it.severity.getNumericValue() }
                            .thenByDescending { it.timestamp }
                            .thenBy { it.id }
                    )
                }
                val previewLimit = 20
                val displayThreats = if (showAllThreats) sortedThreats else {
                    if (sortedThreats.size > previewLimit) sortedThreats.take(previewLimit) else sortedThreats
                }


                // ИСПРАВЛЕНО: Адаптивная высота на основе размера экрана
                // Используем процент от высоты экрана вместо фиксированных значений
                val maxHeight = if (showAllThreats) {
                    // Когда показываем все угрозы, используем большую часть экрана
                    (screenHeightDp * 0.7).dp.coerceAtMost(2000.dp)
                } else {
                    // Для предпросмотра используем адаптивную высоту
                    if (isSmallScreen) 300.dp else 420.dp
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(
                        items = displayThreats,
                        key = { index, threat ->
                            // ИСПРАВЛЕНО: Генерируем уникальный ключ для каждого элемента
                            // Используем комбинацию всех уникальных полей + индекс для гарантии уникальности
                            if (threat.id != 0L) {
                                threat.id
                            } else {
                                // Создаем стабильный уникальный ключ на основе всех полей угрозы
                                "${threat.networkBssid}|${threat.networkSsid}|${threat.type.name}|${threat.timestamp}|${threat.severity.name}|$index"
                            }
                        }
                    ) { index, threat ->
                        ThreatItem(threat = threat)
                    }
                }

                // ИСПРАВЛЕНО: Кнопка разворачивает список, а не перенаправляет
                if (sortedThreats.size > previewLimit) {
                    TextButton(
                        onClick = { showAllThreats = !showAllThreats },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (showAllThreats) "Свернуть" else "Показать все (${sortedThreats.size})")
                    }
                }
            }
        }
    }
}

/**
 * ИСПРАВЛЕНО: Санитизирует SSID и BSSID для безопасного отображения в UI
 * Удаляет потенциально опасные символы и ограничивает длину
 */
private fun sanitizeForDisplay(text: String, maxLength: Int = 32): String {
    if (text.isBlank()) return "Скрытая сеть"
    
    return text
        .take(maxLength)
        .replace(Regex("[<>\"'&;\\\\]"), "") // Удаляем опасные символы
        .trim()
        .ifBlank { "Неизвестно" }
}

@Composable
fun ThreatItem(threat: com.wifiguard.core.domain.model.SecurityThreat) {
    // ИСПРАВЛЕНО: Улучшенное определение темной темы
    // Используем isSystemInDarkTheme() как основной метод, с fallback на calculateLuminance
    val systemDarkTheme = isSystemInDarkTheme()
    val surfaceLuminance = MaterialTheme.colorScheme.surface.calculateLuminance()
    val isDarkTheme = systemDarkTheme || surfaceLuminance < 0.5f
    
    // ИСПРАВЛЕНО: Санитизируем SSID и BSSID перед отображением
    val sanitizedSsid = remember(threat.networkSsid) { sanitizeForDisplay(threat.networkSsid) }
    val sanitizedDescription = remember(threat.description) { sanitizeForDisplay(threat.description, maxLength = 100) }
    
    // Прозрачный фон - элемент сидит прямо на фоне секции
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Используем более контрастные цвета для светлой темы
        val threatColor = when (threat.severity) {
            ThreatLevel.CRITICAL -> if (isDarkTheme) Color(0xFFF44336) else Color(0xFFC62828)
            ThreatLevel.HIGH -> if (isDarkTheme) Color(0xFFFF9800) else Color(0xFFE65100)
            ThreatLevel.MEDIUM -> if (isDarkTheme) Color(0xFFFFC107) else Color(0xFFF57C00)
            ThreatLevel.LOW -> if (isDarkTheme) Color(0xFF8BC34A) else Color(0xFF2E7D32)
            else -> if (isDarkTheme) Color(0xFF607D8B) else Color(0xFF37474F)
        }
        
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = threatColor
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = threat.getShortDescription(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Сеть: $sanitizedSsid",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = sanitizedDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = threat.severity.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = threatColor
        )
    }
}

@Composable
fun RecommendationsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    recommendations: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок секции
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Список рекомендаций (прозрачные элементы внутри секции)
            // ИСПРАВЛЕНО: Используем LazyColumn для больших списков рекомендаций (виртуализация)
            if (expanded) {
                if (recommendations.size > 10) {
                    // Для больших списков используем LazyColumn для виртуализации и оптимизации памяти
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        itemsIndexed(
                            items = recommendations,
                            key = { index, item -> "rec-$index-${item.hashCode()}" }
                        ) { index, recommendation ->
                            RecommendationItem(text = recommendation)
                        }
                    }
                } else {
                    // Для маленьких списков используем простой forEach (меньше overhead)
                    recommendations.forEach { recommendation ->
                        RecommendationItem(text = recommendation)
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationItem(text: String) {
    // Прозрачный фон - элемент сидит прямо на фоне секции
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Lightbulb,
            contentDescription = null,
            tint = Color(0xFFFFC107)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}