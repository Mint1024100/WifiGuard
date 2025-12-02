@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-25
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME установлено на %JAVA_HOME%
echo PATH: %PATH%
echo Проверка версии Java:
java -version
echo Проверка компилятора Java:
javac -version
echo Запуск Gradle wrapper...
"gradle-install\gradle-8.5\bin\gradle.bat" wrapper --gradle-version 8.5
echo Проверка Gradle Wrapper...
.\gradlew.bat --version
pause