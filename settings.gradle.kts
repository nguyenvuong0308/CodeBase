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
        maven(url = uri("https://jitpack.io"))
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = uri("https://jitpack.io"))
    }
}

rootProject.name = "LibCore"

include(":app")
include(":core")
include(":core:ads")
include(":core:analytics")
include(":core:baseui")
include(":core:billing")
include(":core:config")
include(":core:dimens")
include(":core:utilities")
include(":core:preference")
include(":core:rate")
