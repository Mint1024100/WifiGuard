@echo off
setlocal

echo Setting JAVA_HOME to JDK 25...
set JAVA_HOME=C:\Program Files\Java\jdk-25
set PATH=%JAVA_HOME%\bin;%PATH%

echo Checking Java version...
java -version

echo Starting Gradle build with JDK 25...
set JAVA_VERSION=17.0.1
"C:\Program Files\Java\jdk-25\bin\java.exe" ^
  -Djava.version=17 -Djava.vm.specification.version=17 -Djava.specification.version=17 ^
  -cp "gradle\wrapper\gradle-wrapper.jar" ^
  --add-opens=java.base/java.util=ALL-UNNAMED ^
  --add-opens=java.base/java.lang=ALL-UNNAMED ^
  --add-opens=java.base/java.lang.invoke=ALL-UNNAMED ^
  --add-opens=java.prefs/java.util.prefs=ALL-UNNAMED ^
  --add-opens=java.base/java.nio.charset=ALL-UNNAMED ^
  --add-opens=java.base/java.net=ALL-UNNAMED ^
  --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED ^
  -Dorg.gradle.appname=%APP_BASE_NAME% ^
  org.gradle.wrapper.GradleWrapperMain ^
  --warning-mode all ^
  %*

endlocal