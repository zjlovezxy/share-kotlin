pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // repo.maven.apache.org 在 GitHub runner 上偶发全量 404，加一个 Google 托管的
        // Maven Central 镜像作为兜底（内容完全相同）
        maven("https://maven-central.storage-download.googleapis.com/maven2/") {
            name = "centralMirror"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/") {
            name = "centralMirror"
        }
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
