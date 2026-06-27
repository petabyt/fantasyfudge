pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FantasyFudge"
include(":app")

include(":libpak")
project(":libpak").projectDir = File("../pak/android")

include(":library-client-rtsp")
project(":library-client-rtsp").projectDir = File("third_party/rtsp-client-android/library-client-rtsp")