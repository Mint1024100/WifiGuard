@echo off
echo Установка JAVA_HOME на Java 25
set JAVA_HOME=C:\Program Files\Java\jdk-25
echo Обновление PATH
set PATH=%JAVA_HOME%\bin;%PATH%
echo Текущий JAVA_HOME: %JAVA_HOME%
echo Проверка версии Java:
java -version
echo Проверка компилятора Java:
javac -version
echo Запуск Gradle Wrapper...
.\gradlew.bat --version
if %ERRORLEVEL% EQU 0 (
    echo Gradle Wrapper успешно запущен!
    echo Запуск сборки проекта...
    .\gradlew.bat build --dry-run
) else (
    echo Ошибка при запуске Gradle Wrapper
)
pause