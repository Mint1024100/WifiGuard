@echo off
echo Attempting to build with compatibility flags for Java 25

set GRADLE_OPTS=%GRADLE_OPTS% -Dkotlin.environment=jvm -Dkotlin.compiler.execution.strategy=in-process

echo Running gradle with compatibility args...
call .\gradlew.bat clean --no-daemon --warning-mode=all --stacktrace
if %ERRORLEVEL% NEQ 0 (
    echo Build failed with compatibility flags
    exit /b %ERRORLEVEL%
) else (
    echo Build successful!
)