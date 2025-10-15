# Logging System Documentation

## Overview

The WifiGuard application uses a centralized logging system implemented in `Logger.kt`. This system automatically disables verbose logging in release builds while maintaining critical error reporting.

## Features

1. **Automatic Debug Filtering** - Logs are only output in debug builds
2. **Tag Generation** - Automatic tag creation based on calling class
3. **Security Logging** - Special handling for security-related events
4. **Performance Monitoring** - Time measurement utilities
5. **Production Error Reporting** - Integration-ready for crash reporting services

## Usage

### Basic Logging

```kotlin
// Import extension functions
import com.wifiguard.core.common.logd
import com.wifiguard.core.common.loge
import com.wifiguard.core.common.logw

class MyClass {
    fun doSomething() {
        logd("Starting operation")
        
        try {
            // Some operation
            logi("Operation completed successfully")
        } catch (e: Exception) {
            loge("Operation failed", e)
        }
    }
}
```

### Security Logging

```kotlin
import com.wifiguard.core.common.logSecurity

fun checkPermissions() {
    if (!hasPermission()) {
        logSecurity("Unauthorized access attempt detected")
    }
}
```

### Performance Measurement

```kotlin
import com.wifiguard.core.common.Logger

val result = Logger.measureTime("Database Operation") {
    database.expensiveOperation()
}
```

## Log Levels

| Level | Method | Purpose | Release Build |
|-------|--------|---------|---------------|
| DEBUG | `logd()` | Development debugging | Disabled |
| INFO | `logi()` | General information | Disabled |
| WARN | `logw()` | Warning conditions | Disabled |
| ERROR | `loge()` | Error conditions | Disabled* |
| VERBOSE | `logv()` | Verbose debugging | Disabled |
| WTF | `logSecurity()` | Security violations | Always logged |

*ERROR logs are disabled in release builds but exceptions can be sent to crash reporting services.

## Best Practices

### 1. Log Context

Always include enough context to understand what's happening:

```kotlin
// Good
logd("Starting scan for network: ${network.ssid}")

// Bad
logd("Starting scan")
```

### 2. Sensitive Information

Never log sensitive data like passwords, tokens, or personal information:

```kotlin
// Bad - never do this
logd("User password: $password")

// Good - log generic information
logd("Authentication attempt for user: ${user.id}")
```

### 3. Performance Impact

Avoid expensive string operations in log calls:

```kotlin
// Bad - string concatenation always occurs
logd("Processing item " + item.id + " with value " + item.value)

// Good - string concatenation only occurs in debug builds
logd { "Processing item ${item.id} with value ${item.value}" }
```

Currently our logger doesn't support lambda-based lazy evaluation, but this could be added in the future.

### 4. Exception Logging

Always include the exception when logging errors:

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    loge("Failed to perform risky operation", e)
}
```

## Security Logging

Security-related events are always logged regardless of build type:

```kotlin
logSecurity("Suspicious activity detected from IP: ${request.ip}")
```

In production, these events can be sent to security monitoring systems.

## Integration with Crash Reporting

The logger is designed to integrate with crash reporting services like Firebase Crashlytics. Uncomment the relevant lines in the `Logger.kt` file and add the necessary dependencies to enable automatic crash reporting.

## Customization

### Adding New Log Methods

To add new log methods, extend the `Logger` object:

```kotlin
fun Logger.customLog(message: String) {
    // Custom logging implementation
}
```

### Modifying Behavior

The logging behavior can be modified by changing the `isDebug` flag or adding new conditions in the individual log methods.

## Testing

To test logging in release builds:

1. Temporarily set `isDebug = true` in `Logger.kt`
2. Verify log output appears correctly
3. Remember to revert the change before building for production

## Migration from Android Log

To migrate from standard Android logging:

1. Remove `import android.util.Log`
2. Add `import com.wifiguard.core.common.logd` (and others as needed)
3. Replace `Log.d(TAG, message)` with `logd(message)`
4. The class name will automatically be used as the tag

## Future Improvements

1. **Lazy Evaluation** - Add lambda-based lazy string evaluation
2. **Structured Logging** - Add support for structured JSON logs
3. **Remote Logging** - Add remote logging capabilities for beta testing
4. **Log Levels Configuration** - Allow runtime configuration of log levels
5. **Performance Optimization** - Add caching for frequently used tags

## Troubleshooting

### Logs Not Appearing

1. Check that you're running a debug build
2. Verify that the device/emulator logcat is showing output
3. Confirm that filtering isn't hiding your logs
4. Check that you've imported the correct extension functions

### Performance Issues

If logging is causing performance issues:

1. Reduce the frequency of log calls in tight loops
2. Consider removing verbose logging from performance-critical sections
3. Profile the application to confirm logging is the bottleneck