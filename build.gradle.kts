import org.gradle.api.plugins.JavaPluginExtension

plugins {
    id("com.gradleup.shadow") version "9.6.1" apply false
}

subprojects {
    val module = this

    group = "com.team.corporate"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    module.pluginManager.withPlugin("java-base") {
        module.extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
    }
}
