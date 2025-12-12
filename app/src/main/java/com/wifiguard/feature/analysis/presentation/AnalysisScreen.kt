package com.wifiguard.feature.analysis.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.wifiguard.core.ui.theme.calculateLuminance

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
                            onClick = { viewModel.refreshAnalysis() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Повторить")
                        }
                    }
                }
            } else if (uiState.securityReport != null) {
                SecurityReportContent(
                    report = uiState.securityReport!!,
                    onNavigateToFullReport = onNavigateToSecurityReport
                )
            } else {
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
                            onClick = { viewModel.refreshAnalysis() }
                        ) {
                            Text("Обновить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityReportContent(
    report: SecurityReport,
    onNavigateToFullReport: () -> Unit = {}
) {
    val securityScore = report.getOverallSecurityScore()
    var animatedScore by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(securityScore) {
        animatedScore = securityScore.toFloat()
    }

    var threatsExpanded by remember { mutableStateOf(true) }
    var recommendationsExpanded by remember { mutableStateOf(true) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section with Animated Security Score
        item(span = { GridItemSpan(maxLineSpan) }) {
            SecurityScoreHeroSection(
                securityScore = animatedScore,
                overallRiskLevel = report.overallRiskLevel
            )
        }

        // Stats Cards Grid
        item(span = { GridItemSpan(maxLineSpan) }) {
            StatsCardsGrid(
                report = report
            )
        }

        // Threats Section
        if (report.threats.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ThreatsSection(
                    title = "Обнаруженные угрозы",
                    icon = Icons.Rounded.Warning,
                    threats = report.threats,
                    expanded = threatsExpanded,
                    onToggle = { threatsExpanded = !threatsExpanded }
                )
            }
        }

        // Recommendations Section
        if (report.recommendations.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RecommendationsSection(
                    title = "Рекомендации",
                    icon = Icons.Rounded.Lightbulb,
                    recommendations = report.recommendations,
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
                .padding(24.dp),
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
                    .size(200.dp)
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
                        fontSize = 36.sp,  // Reduced from 48sp to 36sp to fit properly in circle
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
                        fontSize = 14.sp,  // 14sp for label
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
                    fontSize = 16.sp,  // 16sp for status
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

            // Список угроз (прозрачные элементы внутри секции)
            if (expanded) {
                threats.forEach { threat ->
                    ThreatItem(threat = threat)
                }
            }
        }
    }
}

@Composable
fun ThreatItem(threat: com.wifiguard.core.domain.model.SecurityThreat) {
    // Определяем темную тему по яркости фона
    val isDarkTheme = MaterialTheme.colorScheme.surface.calculateLuminance() < 0.5f
    
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
                text = "Сеть: ${threat.networkSsid}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = threat.description,
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
            if (expanded) {
                recommendations.forEach { recommendation ->
                    RecommendationItem(text = recommendation)
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