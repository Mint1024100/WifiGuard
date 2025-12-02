// init.gradle.kts
// Скрипт инициализации для обхода проблемы с определением версии Java 25

// Установка системного свойства до инициализации Kotlin
System.setProperty("java.version", "17.0.8")  // Установка версии Java для совместимости
System.setProperty("java.runtime.version", "17.0.8")

println("Установка поддельной версии Java для совместимости с Kotlin")