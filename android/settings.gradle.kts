pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "CopilotDriveAI"
include(":core", ":testing")
if (!providers.gradleProperty("coreOnly").isPresent) include(":app")
