pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Chronix"
include(":shared")
include(":app")
// Librería de Magic Friends (repo hermano, ver docs/SPEC.md §9). En CI se clona en ../magicfriends (ios.yml).
includeBuild("../magicfriends/client")
