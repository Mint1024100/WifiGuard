# Instructions to Run ThreatNotificationWorkerTest

The test `com.wifiguard.core.background.ThreatNotificationWorkerTest` is an Android instrumentation test that requires a connected Android device or emulator to run.

## Prerequisites

1. Install Android SDK Platform Tools
2. Ensure `adb` is in your PATH
3. Connect an Android device via USB (with developer options and USB debugging enabled) OR start an Android emulator

## Steps to Run the Test

### 1. Verify Device Connection
```bash
adb devices
```
You should see at least one connected device.

### 2. Run the Specific Test
```bash
cd /Users/mint1024/Desktop/андроид
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.wifiguard.core.background.ThreatNotificationWorkerTest
```

### Alternative: Run All Android Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

## Troubleshooting

If you still encounter issues with the Gradle wrapper, try:

1. Ensure JAVA_HOME is properly set
2. Try with the workaround options:
```bash
JAVA_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED" GRADLE_OPTS="-Dkotlin.compiler.execution.strategy=in-process" ./gradlew --no-daemon connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.wifiguard.core.background.ThreatNotificationWorkerTest
```

Note: The emergency Gradle wrapper in this project might have compatibility issues with newer Gradle commands.