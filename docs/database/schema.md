# Database Documentation - WifiGuard

## Overview

WifiGuard использует Room - объектно-реляционное отображение (ORM) для работы с SQLite базой данных. Все данные хранятся локально на устройстве пользователя без передачи на внешние серверы.

## Database Configuration

### Connection Settings

```kotlin
// Constants.kt
DATABASE_NAME = "wifiguard_database"
DATABASE_VERSION = 1
```

### Initialization

База данных инициализируется через Hilt в `DataModule.kt`:

```kotlin
@Singleton
@Provides
fun provideDatabase(@ApplicationContext context: Context): WifiGuardDatabase {
    return Room.databaseBuilder(
        context,
        WifiGuardDatabase::class.java,
        Constants.DATABASE_NAME
    )
    .addMigrations() // Будущие миграции
    .build()
}
```

## Schema Definition

### NetworkEntity

**Таблица**: `wifi_networks`

| Поле | Тип | Описание |
|------|-----|----------|
| id | TEXT (Primary Key) | Уникальный идентификатор сети |
| ssid | TEXT | Имя сети (SSID) |
| bssid | TEXT | MAC-адрес точки доступа |
| securityType | TEXT | Тип безопасности (WEP, WPA, WPA2, WPA3, Open) |
| signalStrength | INTEGER | Уровень сигнала в dBm |
| frequency | INTEGER | Частота в MHz |
| channel | INTEGER | Номер канала |
| timestamp | INTEGER | Время последнего сканирования (timestamp) |
| threatLevel | TEXT | Уровень угрозы (Safe, Low, Medium, High, Critical) |
| threatTypes | TEXT | JSON строка с типами угроз |

### ScanResultEntity

**Таблица**: `scan_results`

| Поле | Тип | Описание |
|------|-----|----------|
| id | TEXT (Primary Key) | Уникальный идентификатор результата сканирования |
| timestamp | INTEGER | Время сканирования |
| networkCount | INTEGER | Количество найденных сетей |
| threatCount | INTEGER | Количество сетей с угрозами |
| summary | TEXT | JSON строка с кратким отчетом |

### ThreatEntity

**Таблица**: `threats`

| Поле | Тип | Описание |
|------|-----|----------|
| id | TEXT (Primary Key) | Уникальный идентификатор угрозы |
| networkId | TEXT | Ссылка на сеть (Foreign Key) |
| type | TEXT | Тип угрозы (OpenNetwork, WeakEncryption, etc.) |
| level | TEXT | Уровень угрозы |
| description | TEXT | Описание угрозы |
| timestamp | INTEGER | Время обнаружения |

### SecurityReportEntity

**Таблица**: `security_reports`

| Поле | Тип | Описание |
|------|-----|----------|
| id | TEXT (Primary Key) | Уникальный идентификатор отчета |
| timestamp | INTEGER | Время создания отчета |
| overallScore | INTEGER | Общий балл безопасности (0-100) |
| threatsFound | INTEGER | Общее количество угроз |
| safeNetworks | INTEGER | Количество безопасных сетей |
| totalNetworks | INTEGER | Общее количество проанализированных сетей |
| recommendations | TEXT | JSON строка с рекомендациями |

## DAO (Data Access Objects)

### NetworkDao

```kotlin
// Operations:
- getAllNetworks(): Flow<List<NetworkEntity>>
- getNetworkById(id: String): Flow<NetworkEntity?>
- getNetworkBySsid(ssid: String): Flow<List<NetworkEntity>>
- insertNetwork(network: NetworkEntity)
- updateNetwork(network: NetworkEntity)
- deleteNetwork(id: String)
- deleteAllNetworks()
- getUnsafeNetworks(): List<NetworkEntity>
- getNetworksByThreatLevel(level: String): List<NetworkEntity>
```

### ScanResultDao

```kotlin
// Operations:
- getAllScanResults(): Flow<List<ScanResultEntity>>
- getScanResultById(id: String): Flow<ScanResultEntity?>
- insertScanResult(result: ScanResultEntity)
- deleteScanResult(id: String)
- deleteOldScanResults(olderThan: Long) // Для очистки данных
- getLastScanResult(): ScanResultEntity?
```

### ThreatDao

```kotlin
// Operations:
- getAllThreats(): Flow<List<ThreatEntity>>
- getThreatsByNetworkId(networkId: String): Flow<List<ThreatEntity>>
- getThreatsByType(type: String): Flow<List<ThreatEntity>>
- getThreatById(id: String): Flow<ThreatEntity?>
- insertThreat(threat: ThreatEntity)
- updateThreat(threat: ThreatEntity)
- deleteThreat(id: String)
- deleteThreatsForNetwork(networkId: String)
- getActiveThreats(): List<ThreatEntity>
```

## Data Security

### Encryption

Чувствительные данные в базе данных зашифрованы с использованием:

- **AES-256-GCM** шифрования
- **Android Keystore** для безопасного хранения ключей
- **HMAC-SHA256** для проверки целостности

### Key Management

```kotlin
// Constants.kt
KEYSTORE_PROVIDER = "AndroidKeyStore"
AES_KEY_ALIAS = "WifiGuardAESKey"
HMAC_KEY_ALIAS = "WifiGuardHMACKey"
```

### Local Storage Only

- Все данные хранятся исключительно на устройстве
- Никакие данные не передаются на внешние серверы
- Пользователь может очистить все данные в любое время
- Поддержка экспорта/импорта данных (локально)

## Migration Strategy

### Current Version

- **Database Version**: 1 (initial schema)
- **Migration Policy**: Explicit migrations required
- **No fallbackToDestructiveMigration()** to prevent data loss

### Future Migration Guidelines

When schema changes are needed:

1. **Never use destructive migrations** - user data must be preserved
2. **Always use explicit migrations** with proper SQL statements
3. **Test migrations thoroughly** using MigrationTestHelper
4. **Maintain backward compatibility** where possible

### Example Migration Structure

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            // Safe migration operations
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}
```

## Performance Optimization

### Indexes

```sql
-- Indexes for performance
CREATE INDEX index_wifi_networks_ssid ON wifi_networks(ssid);
CREATE INDEX index_wifi_networks_threat_level ON wifi_networks(threatLevel);
CREATE INDEX index_wifi_networks_timestamp ON wifi_networks(timestamp);
CREATE INDEX index_threats_network_id ON threats(networkId);
CREATE INDEX index_scan_results_timestamp ON scan_results(timestamp);
```

### Data Retention

- **Default retention**: 30 days (configurable in settings)
- **Maximum retention**: 365 days
- **Automatic cleanup**: Daily cleanup of old data via DataCleanupWorker
- **Manual cleanup**: Available in settings

### Query Optimization

- Use `Flow` for reactive UI updates
- Implement pagination for large datasets
- Cache frequently accessed data
- Use batch operations where possible

## Backup and Restore

### Android Backup Rules

```xml
<!-- In AndroidManifest.xml -->
android:allowBackup="false"
android:fullBackupContent="false"
```

**Rationale**: 
- Data is location/network sensitive
- Privacy considerations
- Security implications
- Data is regenerated on each scan

### Data Extraction Rules

```xml
<!-- In res/xml/data_extraction_rules.xml -->
<cloud-backup>
    <exclude domain="database" path="wifiguard_database" />
</cloud-backup>
```

## Troubleshooting

### Common Issues

1. **Database locked**: Ensure all database operations are performed off the main thread
2. **Migration failures**: Always test migrations with real data before release  
3. **Disk space**: Implement data retention policies to prevent excessive storage usage
4. **Concurrent access**: Use proper synchronization for multi-threaded access

### Debugging Database

```bash
# Check database on device (requires root or debug app)
adb shell run-as com.wifiguard cat databases/wifiguard_database

# View database schema
adb shell run-as com.wifiguard sqlite3 databases/wifiguard_database ".schema"
```

### Database Inspector

Use Android Studio's Database Inspector for debugging:
1. Run the debug version of the app
2. Open View → Tool Windows → Database Inspector
3. Select the WifiGuard process
4. Inspect tables and execute queries in real-time

## Schema Export

Database schemas are exported to:
- `app/schemas/` directory
- Version-controlled for migration reference
- Used for verifying schema changes

## Data Types and Validation

### Threat Levels

- Safe (Safe)
- Low (Low risk)
- Medium (Medium risk) 
- High (High risk)
- Critical (Critical risk)

### Security Types

- Open (No encryption)
- WEP (Wired Equivalent Privacy)
- WPA (Wi-Fi Protected Access)
- WPA2 (Wi-Fi Protected Access 2)
- WPA3 (Wi-Fi Protected Access 3)
- WPA2_WPA3 (Compatible mode)
- EAP (Extensible Authentication Protocol)
- Unknown (Unrecognized)

### Signal Strength (dBm)

- Excellent: -30 to -50 dBm
- Good: -51 to -60 dBm
- Fair: -61 to -70 dBm
- Weak: -71 to -80 dBm
- Very Weak: -81 to -90 dBm
- Critical: -91 to -100 dBm