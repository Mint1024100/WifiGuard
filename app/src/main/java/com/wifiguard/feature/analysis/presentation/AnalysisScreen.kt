package com.wifiguard.feature.analysis.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wifiguard.core.ui.components.SecurityStatusIndicator
import com.wifiguard.core.ui.components.NetworkStatsCard
import com.wifiguard.core.ui.theme.*
import com.wifiguard.feature.scanner.presentation.SecurityAnalysisViewModel
import com.wifiguard.feature.scanner.presentation.ThreatFilter
import com.wifiguard.feature.scanner.presentation.ThreatSeverity

/**
 * Экран анализа безопасности Wi-Fi сетей
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    networkId: String,
    onNavigateBack: () -> Unit,
    viewModel: SecurityAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val securityAnalysis by viewModel.securityAnalysis.collectAsStateWithLifecycle()
    val securityStatistics by viewModel.securityStatistics.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Анализ безопасности") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startSecurityAnalysis() }) {
                        Icon(Icons.Default.Refresh, "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isAnalyzing) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Анализ безопасности...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Security score card
                    item {
                        securityStatistics?.let { stats ->
                            SecurityScoreCard(
                                score = stats.securityScore,
                                totalThreats = stats.totalThreats,
                                highSeverityThreats = stats.highSeverityThreats
                            )
                        }
                    }
                    
                    // Network statistics
                    item {
                        securityStatistics?.let { stats ->
                            NetworkStatsCard(
                                totalNetworks = stats.totalNetworks,
                                secureNetworks = stats.secureNetworks,
                                openNetworks = stats.openNetworks
                            )
                        }
                    }
                    
                    // Filter chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.threatFilter == ThreatFilter.ALL,
                                onClick = { viewModel.updateThreatFilter(ThreatFilter.ALL) },
                                label = { Text("Все") }
                            )
                            FilterChip(
                                selected = uiState.threatFilter == ThreatFilter.HIGH,
                                onClick = { viewModel.updateThreatFilter(ThreatFilter.HIGH) },
                                label = { Text("Высокие") }
                            )
                            FilterChip(
                                selected = uiState.threatFilter == ThreatFilter.MEDIUM,
                                onClick = { viewModel.updateThreatFilter(ThreatFilter.MEDIUM) },
                                label = { Text("Средние") }
                            )
                            FilterChip(
                                selected = uiState.threatFilter == ThreatFilter.LOW,
                                onClick = { viewModel.updateThreatFilter(ThreatFilter.LOW) },
                                label = { Text("Низкие") }
                            )
                        }
                    }
                    
                    // Threats header
                    item {
                        Text(
                            text = "Обнаруженные угрозы (${securityAnalysis.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Threats list
                    if (securityAnalysis.isEmpty()) {
                        item {
                            EmptyThreatsState()
                        }
                    } else {
                        items(
                            items = securityAnalysis.filter {
                                when (uiState.threatFilter) {
                                    ThreatFilter.ALL -> true
                                    ThreatFilter.HIGH -> it.severity == ThreatSeverity.HIGH
                                    ThreatFilter.MEDIUM -> it.severity == ThreatSeverity.MEDIUM
                                    ThreatFilter.LOW -> it.severity == ThreatSeverity.LOW
                                }
                            },
                            key = { "${it.networkSsid}_${it.type}" }
                        ) { threat ->
                            ThreatCard(threat = threat)
                        }
                    }
                }
            }
            
            // Error message
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
private fun SecurityScoreCard(
    score: Int,
    totalThreats: Int,
    highSeverityThreats: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                score >= 80 -> SecureGreen.copy(alpha = 0.1f)
                score >= 50 -> WarningOrange.copy(alpha = 0.1f)
                else -> DangerRed.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Оценка безопасности",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 12.dp,
                    color = when {
                        score >= 80 -> SecureGreen
                        score >= 50 -> WarningOrange
                        else -> DangerRed
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            score >= 80 -> SecureGreen
                            score >= 50 -> WarningOrange
                            else -> DangerRed
                        }
                    )
                    Text(
                        text = "из 100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalThreats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Всего угроз",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$highSeverityThreats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed
                    )
                    Text(
                        text = "Критических",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreatCard(
    threat: com.wifiguard.feature.scanner.presentation.SecurityThreat
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Severity indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (threat.severity) {
                            ThreatSeverity.HIGH -> DangerRed
                            ThreatSeverity.MEDIUM -> WarningOrange
                            ThreatSeverity.LOW -> Color(0xFFFFC107)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (threat.severity) {
                        ThreatSeverity.HIGH -> Icons.Default.Error
                        ThreatSeverity.MEDIUM -> Icons.Default.Warning
                        ThreatSeverity.LOW -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = threat.networkSsid,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = threat.type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = threat.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = threat.recommendation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyThreatsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SecureGreen
            )
            Text(
                text = "Угроз не обнаружено",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SecureGreen
            )
            Text(
                text = "Все сети безопасны",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


