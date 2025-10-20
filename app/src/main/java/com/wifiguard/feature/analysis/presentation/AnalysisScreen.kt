package com.wifiguard.feature.analysis.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.security.SecurityReport
import androidx.compose.foundation.lazy.items

/**
 * Экран анализа безопасности
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Анализ безопасности") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ошибка загрузки данных",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { viewModel.refreshAnalysis() }
                        ) {
                            Text("Повторить")
                        }
                    }
                }
            } else if (uiState.securityReport != null) {
                SecurityReportContent(uiState.securityReport!!)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет данных для анализа",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Button(
                        onClick = { viewModel.refreshAnalysis() }
                    ) {
                        Text("Обновить")
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityReportContent(report: SecurityReport) {
    LazyColumn {
        item {
            // Общий отчет безопасности
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Общий отчет безопасности",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Всего сетей: ${report.totalNetworks}")
                            Text("Безопасных: ${report.safeNetworks}")
                            Text("Низкий риск: ${report.lowRiskNetworks}")
                        }
                        Column {
                            Text("Средний риск: ${report.mediumRiskNetworks}")
                            Text("Высокий риск: ${report.highRiskNetworks}")
                            Text("Критический: ${report.criticalRiskNetworks}")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Общий уровень риска: ${report.overallRiskLevel.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = "Оценка безопасности: ${report.getOverallSecurityScore()}/100",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        // Угрозы
        if (report.threats.isNotEmpty()) {
            item {
                Text(
                    text = "Обнаруженные угрозы",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
            }
            
            items(report.threats) { threat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = threat.getShortDescription(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Сеть: ${threat.networkSsid}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Описание: ${threat.description}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Уровень: ${threat.severity.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // Рекомендации
        if (report.recommendations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Рекомендации",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(report.recommendations) { recommendation ->
                ListItem(
                    headlineContent = {
                        Text(recommendation)
                    }
                )
            }
        }
    }
}