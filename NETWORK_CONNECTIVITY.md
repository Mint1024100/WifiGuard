# Network Connectivity Monitoring System

## Overview

The WifiGuard application includes a comprehensive network connectivity monitoring system to track internet availability and connection types. This system is designed to provide real-time updates about network status and help the application adapt its behavior based on connectivity.

## Components

### 1. NetworkMonitor

The core component that monitors network connectivity using Android's ConnectivityManager APIs.

**Key Features:**
- Real-time network status monitoring
- Connection type detection (Wi-Fi, Cellular, Ethernet)
- Callback-based architecture with coroutines
- Backward compatibility for older Android versions

### 2. NetworkConnectivityViewModel

A Hilt-managed ViewModel that exposes network state to UI components.

**Key Features:**
- StateFlow for reactive UI updates
- Combined network status and connection type
- Freshness tracking (last updated timestamp)
- Easy integration with Compose and Views

### 3. NetworkConnectivityDialog

A reusable Compose dialog component for displaying network status to users.

**Key Features:**
- Adaptive messaging based on connection status
- Connection type information
- Helpful troubleshooting tips
- Consistent Material Design 3 styling

## Implementation Details

### Network Status Detection

The system detects network connectivity through:

1. **Active Network Capabilities** - Checks for validated internet capability
2. **Transport Type** - Determines Wi-Fi, cellular, or ethernet
3. **Connection Validation** - Ensures actual internet access (not just network availability)

### Real-time Monitoring

Uses ConnectivityManager.NetworkCallback to receive:

- `onAvailable()` - Network becomes available
- `onLost()` - Network connection lost
- `onCapabilitiesChanged()` - Network capabilities updated
- `onUnavailable()` - Network becomes unavailable

### State Management

Maintains state through:

- `StateFlow<NetworkConnectivityState>` for reactive updates
- Combines online status and connection type
- Tracks last update timestamp for freshness
- Provides utility methods for common checks

## Usage Examples

### In ViewModel

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val networkConnectivityViewModel: NetworkConnectivityViewModel
) : ViewModel() {
    
    val networkStatus = networkConnectivityViewModel.networkStatus
    
    fun performNetworkOperation() {
        if (networkStatus.value.isOnline) {
            // Perform online operation
        } else {
            // Handle offline scenario
        }
    }
}
```

### In Compose UI

```kotlin
@Composable
fun MyScreen(
    networkConnectivityViewModel: NetworkConnectivityViewModel = hiltViewModel()
) {
    val networkStatus by networkConnectivityViewModel.networkStatus.collectAsState()
    
    LaunchedEffect(networkStatus) {
        if (!networkStatus.isOnline) {
            // Show network connectivity dialog
        }
    }
    
    // Rest of UI
}
```

### Using NetworkConnectivityDialog

```kotlin
@Composable
fun NetworkStatusDialog(
    networkStatus: NetworkConnectivityState,
    onDismiss: () -> Unit
) {
    NetworkConnectivityDialog(
        isConnected = networkStatus.isOnline,
        connectionType = networkStatus.connectionType,
        onDismiss = onDismiss,
        onRetry = { /* Retry network operation */ }
    )
}
```

## Permissions

The system requires the following permissions in AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

Note: Internet permission is only required if the app performs actual network operations.

## Backward Compatibility

The system supports:

- **Android 6.0+** - Full feature set
- **Android 5.0-5.1** - Limited capability checking
- **Android 4.4 and below** - Deprecated API fallback

## Performance Considerations

1. **Efficient Callbacks** - Only registers necessary callbacks
2. **Distinct Updates** - Prevents duplicate state emissions
3. **Resource Cleanup** - Properly unregisters callbacks
4. **Main Thread Safe** - All updates on main thread

## Testing

### Unit Tests

```kotlin
@Test
fun `network monitor detects online status`() {
    // Mock ConnectivityManager
    // Test various network scenarios
    // Verify state emissions
}
```

### Instrumentation Tests

```kotlin
@Test
fun `network connectivity viewmodel updates correctly`() {
    // Test with actual network changes
    // Verify state flow updates
    // Check freshness timestamps
}
```

## Integration with Other Systems

### Battery Optimization

The network monitoring system is designed to be battery-friendly:

- Uses system callbacks rather than polling
- Automatically registers/unregisters based on lifecycle
- Minimal processing in callback handlers

### WorkManager Integration

Can be integrated with background tasks:

```kotlin
class NetworkDependentWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Check network before performing work
        return if (isNetworkAvailable()) {
            // Perform network operation
            Result.success()
        } else {
            Result.retry()
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **False Online Status**
   - Check for captive portals
   - Validate actual internet access
   - Consider DNS resolution tests

2. **Missing Callbacks**
   - Verify permission declarations
   - Check network callback registration
   - Ensure proper cleanup

3. **Performance Problems**
   - Monitor callback frequency
   - Optimize state processing
   - Consider debouncing frequent updates

### Debugging Tips

1. Use Android Studio's Network Profiler
2. Log network state changes with timestamps
3. Test with various connection scenarios
4. Monitor battery usage during network monitoring

## Future Enhancements

### Planned Features

1. **Bandwidth Detection** - Measure connection speed
2. **Roaming Detection** - Identify cellular roaming
3. **VPN Detection** - Detect VPN connections
4. **Proxy Detection** - Identify proxy configurations

### Advanced Monitoring

1. **Connection Quality** - Beyond simple online/offline
2. **Latency Measurement** - Ping-based latency detection
3. **Bandwidth Testing** - Throughput measurement
4. **Network Prediction** - Predict network availability

## Security Considerations

1. **Minimal Permissions** - Only request necessary permissions
2. **No Personal Data** - Avoid collecting personal network information
3. **Secure Communication** - Use HTTPS for network operations
4. **Privacy Respecting** - Don't track user browsing habits

## Best Practices

### 1. Efficient Resource Usage

```kotlin
// Register callbacks only when needed
override fun onResume() {
    super.onResume()
    networkMonitor.startMonitoring()
}

override fun onPause() {
    super.onPause()
    networkMonitor.stopMonitoring()
}
```

### 2. Graceful Degradation

```kotlin
when {
    networkStatus.isOnline -> {
        // Enable online features
    }
    networkStatus.connectionType == ConnectionType.WIFI -> {
        // Enable limited features for Wi-Fi
    }
    else -> {
        // Disable network-dependent features
    }
}
```

### 3. User Experience

- Provide clear network status indicators
- Offer helpful troubleshooting suggestions
- Cache data for offline scenarios when possible
- Inform users about required network operations

## Conclusion

The network connectivity monitoring system provides a robust foundation for handling network-dependent operations in WifiGuard. By combining real-time monitoring with reactive state management, the system enables responsive UI updates and adaptive application behavior based on network conditions.