@echo off
echo Попытка использовать альтернативную Java-среду для обхода проблемы с Java 25

REM Проверяем наличие Java 17 в системе
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    echo Найдена Java 17, пробуем использовать её для сборки
    set JAVA_HOME=C:\Program Files\Java\jdk-17
    set PATH=C:\Program Files\Java\jdk-17\bin;%PATH%
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.0.0\bin\java.exe" (
    echo Найдена Java 17 от Eclipse Adoptium, пробуем использовать её для сборки
    set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.0.0
    set PATH=C:\Program Files\Eclipse Adoptium\jdk-17.0.0.0\bin;%PATH%
) else (
    echo Не найдена Java 17, пробуем использовать системную переменную JAVA_HOME
    REM Пробуем использовать системную переменную и указать для Kotlin использовать Java 17
    set JAVA_OPTS=-Djava.version=17 -Djava.vm.specification.version=17 -Djava.specification.version=17
)

echo Текущая версия Java:
java -version

echo Запуск Gradle с обходом проблемы...
"%JAVA_HOME%\bin\java.exe" -Djava.version=17 -Djava.vm.specification.version=17 -Djava.specification.version=17 -cp "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain clean --warning-mode=all

if %ERRORLEVEL% EQU 0 (
    echo Очистка прошла успешно!
    echo Запуск сборки...
    "%JAVA_HOME%\bin\java.exe" -Djava.version=17 -Djava.vm.specification.version=17 -Djava.specification.version=17 -cp "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain assembleDebug --warning-mode=all
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