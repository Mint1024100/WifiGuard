@echo off
echo Настройка сборки с обходом проблемы с Java 25

set JAVA_OPTS=%JAVA_OPTS% --add-opens=java.base/java.lang=ALL-UNNAMED
set GRADLE_OPTS=%GRADLE_OPTS% -Dkotlin.compiler.execution.strategy=in-process -Djrt.scan.policy=fs

echo Попытка очистки проекта...
.\gradlew.bat clean --no-daemon --warning-mode=all --stacktrace

if %ERRORLEVEL% EQU 0 (
    echo Очистка прошла успешно!
    echo Попытка сборки проекта...
    .\gradlew.bat assembleDebug --no-daemon --warning-mode=all --stacktrace
    if %ERRORLEVEL% EQU 0 (
        echo Сборка прошла успешно!
        echo APK файлы находятся в папке app\build\outputs\apk\
        dir app\build\outputs\apk\*.apk /s
    ) else (
        echo Ошибка при сборке проекта
        pause
        exit /b %ERRORLEVEL%
    )
) else (
    echo Ошибка при очистке проекта
    pause
    exit /b %ERRORLEVEL%
)