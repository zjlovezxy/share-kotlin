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

rootProject.name = "share-kotlin"

include(":app")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:data")
include(":core:auth")
include(":core:ui")
include(":feature:timeline")
include(":feature:detail")
include(":feature:publish")
include(":feature:search")
include(":feature:profile")
include(":feature:message")
include(":feature:notification")
include(":feature:settings")
