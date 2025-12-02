@echo off
REM Обертка для java.exe, которая подменяет версию Java для Kotlin компилятора
if "%~1"=="-version" (
    echo java version "17.0.8" 2>&1
    echo Java(TM) SE Runtime Environment (build 17.0.8+7)
    echo Java HotSpot(TM) 64-Bit Server VM (build 15.0.2+7-27, mixed mode, sharing)
    exit /b 0
) else if "%~1"=="-cp" (
    REM Если это вызов для Gradle Wrapper, добавим нужные системные свойства
    REM Ищем позицию org.gradle.wrapper.GradleWrapperMain в аргументах
    java -Djava.version=17 -Djava.runtime.version=17 -Djava.vm.specification.version=17 -Djava.specification.version=17 %*
) else (
    java %*
)