pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack для библиотек GitHub
        maven { url = uri("https://jitpack.io") }
    }
}

// Каталог libs автоматически загружается из gradle/libs.versions.toml

rootProject.name = "WifiGuard"
include(":app")