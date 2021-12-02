plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("org.jetbrains.dokka")
    id("com.gradle.plugin-publish") version Versions.gradlePluginPublish
}


gradlePlugin {
    (plugins) {
        create("marathon-gradle-plugin") {
            id = "marathon"
            displayName = "Gradle plugin for Marathon test runner"
            description = "Marathon is a fast and platform-independent test runner focused on performance and stability"
            implementationClass = "com.malinskiy.marathon.gradle.MarathonPlugin"
        }
    }
}

pluginBundle {
    website = "https://marathonlabs.github.io/marathon/"
    vcsUrl = "https://github.com/MarathonLabs/marathon"
    tags = listOf("marathon", "test", "runner", "android")
}

Deployment.initialize(project)

dependencies {
    implementation(gradleApi())
    implementation(Libraries.kotlinLogging)
    implementation(project(":configuration"))
    implementation(BuildPlugins.androidGradle)
    implementation(Libraries.apacheCommonsCodec)
}

tasks.processResources.configure {
    from(rootProject.project("cli").layout.buildDirectory.dir("distributions").get().asFile) {
        rename {
            if (it.endsWith(".zip") && it.contains("marathon")) {
                "marathon-cli.zip"
            } else {
                it
            }
        }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(rootProject.project("cli").tasks.getByName("distZip"))
}
