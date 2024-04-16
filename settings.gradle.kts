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
        maven {
            url = uri("https://maven.google.com/") // Use https instead of http and use uri() instead of single quotes
        }
    }
}

rootProject.name = "MaketApplication"
include(":app")
 