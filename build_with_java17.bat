@echo off
setlocal

echo Установка JAVA_HOME на Java 17
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Проверка версии Java:
java -version

echo Запуск сборки проекта...
.\gradlew.bat clean
if %ERRORLEVEL% EQU 0 (
    echo Очистка прошла успешно!
    .\gradlew.bat assembleDebug
    if %ERRORLEVEL% EQU 0 (
        echo Сборка прошла успешно!
        echo APK файлы находятся в папке app\build\outputs\apk\
        dir app\build\outputs\apk\*.apk /s
    ) else (
        echo Ошибка при сборке
        pause
        exit /b 1
    )
) else (
    echo Ошибка при очистке проекта
    pause
    exit /b 1
)

endlocal