package com.wifiguard.feature.analysis.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

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
                            imageVector = Icons.Filled.ArrowBack,
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
        floatingActionButton = {
            if (uiState.securityReport != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp) // Add bottom padding to position it properly
                ) {
                    Surface(
                        color = Color(0xFF34A853), // Vibrant green
                        tonalElevation = 2.dp,
                        shape = RoundedCornerShape(28.dp), // Pill shape with 28dp corner radius
                        modifier = Modifier
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { onNavigateToSecurityReport() }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = "Полный отчет",
                                tint = Color.White, // White icon
                                modifier = Modifier.size(24.dp) // 24dp icon size
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // 8dp spacing between icon and text
                            Text(
                                text = "Полный отчет",
                                fontSize = 16.sp,
                                fontWeight = FontWeight(600), // Bold
                                color = Color.White // White text
                            )
                        }
                    }
                }
            }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section with Animated Security Score
        item {
            SecurityScoreHeroSection(
                securityScore = animatedScore,
                overallRiskLevel = report.overallRiskLevel
            )
        }

        // Stats Cards Grid
        item {
            StatsCardsGrid(
                report = report
            )
        }

        // Threats Section
        if (report.threats.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Обнаруженные угрозы",
                    icon = Icons.Rounded.Warning
                )
            }

            items(report.threats) { threat ->
                ThreatCard(threat = threat)
            }
        }

        // Recommendations Section
        if (report.recommendations.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Рекомендации",
                    icon = Icons.Rounded.Lightbulb
                )
            }

            items(report.recommendations) { recommendation ->
                RecommendationItem(text = recommendation)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
        }
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
            containerColor = Color(0xFFF5F5F5), // Subtle background tint: #F5F5F5
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
    Spacer(modifier = Modifier.height(32.dp)) // 32dp bottom margin before "Полный отчет" button
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
            containerColor = Color(0xFFF5F5F5), // Subtle background tint: #F5F5F5
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
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
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
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ThreatCard(threat: com.wifiguard.core.domain.model.SecurityThreat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when (threat.severity) {
                ThreatLevel.CRITICAL -> Color(0xFFFFEBEE).copy(alpha = 0.6f)
                ThreatLevel.HIGH -> Color(0xFFFFF3E0).copy(alpha = 0.6f)
                ThreatLevel.MEDIUM -> Color(0xFFFFF8E1).copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val threatColor = when (threat.severity) {
                ThreatLevel.CRITICAL -> Color(0xFFF44336)
                ThreatLevel.HIGH -> Color(0xFFFF9800)
                ThreatLevel.MEDIUM -> Color(0xFFFFC107)
                else -> Color(0xFF607D8B)
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
}

@Composable
fun RecommendationItem(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
}