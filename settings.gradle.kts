pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jcenter() // Optional, as it’s being phased out

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter() // Optional, as it’s being phased out
        maven (url ="https://jitpack.io")

    }
}

rootProject.name = "FABLAB"
include(":app")
 