# Database Versioning and Migrations Guide

## Overview

This document describes the database versioning strategy for WifiGuard application using Room persistence library.

## Current Database Schema

### Version 1 (Initial Release)
- `wifi_scans` table
- `wifi_networks` table
- `threats` table
- `scan_sessions` table

### Version 2 (Added Vendor Information)
- Added `vendor` column to `wifi_networks` table
- Added `channel` column to `wifi_networks` table

### Version 3 (Enhanced Threat Tracking)
- Added `resolved_timestamp` column to `threats` table

### Version 4 (Settings Storage)
- Created `settings` table for application preferences

## Migration Strategy

### Safe Migration Practices

1. **Always increment version number** when making schema changes
2. **Write migration tests** before releasing updates
3. **Never use destructive fallback** in production
4. **Document all schema changes** in this file

### Migration Implementation

Migrations are defined in `WifiGuardDatabase.kt`:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add vendor field
        database.execSQL(
            "ALTER TABLE wifi_networks ADD COLUMN vendor TEXT"
        )
        
        // Add channel field
        database.execSQL(
            "ALTER TABLE wifi_networks ADD COLUMN channel INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

### Testing Migrations

Before each release:
1. Create database with previous version
2. Insert sample data
3. Apply migration
4. Verify data integrity
5. Check new fields/columns

## Best Practices

### 1. Schema Changes
- Only add nullable columns or columns with defaults
- Never remove columns without proper migration
- Keep backward compatibility when possible

### 2. Version Control
- Export schema to `/schemas` directory
- Review schema diffs during code reviews
- Maintain changelog of all database changes

### 3. Error Handling
- Provide meaningful error messages for migration failures
- Log migration attempts and results
- Consider graceful degradation for non-critical features

## Future Considerations

### Planned Schema Updates
- Add geolocation tracking for networks (optional)
- Enhanced threat categorization
- Performance optimizations for large datasets

### Deprecation Policy
- Support at least 2 previous schema versions
- Provide data export/import functionality
- Notify users of major schema changes

## Troubleshooting

### Common Issues

1. **Migration Not Found**
   - Check version numbers in `@Database` annotation
   - Verify migration is added to `addMigrations()`

2. **Data Loss During Migration**
   - Never use `fallbackToDestructiveMigration()` in production
   - Test migrations with real user data samples

3. **Performance Issues**
   - Run migrations on background thread
   - Optimize complex migration queries
   - Consider incremental migrations for large datasets

### Recovery Procedures

1. **Rollback Failed Migration**
   - Restore from last known good backup
   - Clear app data and restart (loses user data)
   - Contact support for advanced recovery

2. **Handle Migration Exceptions**
   ```kotlin
   try {
       // Migration code
   } catch (e: Exception) {
       // Log error
       // Provide user-friendly message
       // Offer data recovery options
   }
   ```

## References

- [Room Migrations Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [SQLite ALTER TABLE](https://www.sqlite.org/lang_altertable.html)
- [Android Database Best Practices](https://developer.android.com/topic/libraries/architecture/room)