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
            url = uri("https://git.netpeak.net/api/v4/projects/6017/packages/maven")

            credentials(HttpHeaderCredentials::class) {
                name = "Private-Token"
                value = "glpat-yK1E68yPkpxaViQ-5fG-"
            }

            authentication {
                create<HttpHeaderAuthentication>("header")
            }

            name = "Gitlab"
        }
    }
}

rootProject.name = "KMA Android SDK"
include(":app")
include(":kma")