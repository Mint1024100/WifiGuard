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
echo Запуск проверки Gradle Wrapper...
.\gradlew.bat --version
pause