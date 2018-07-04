import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension

object Deployment {
    val user = System.getenv("SONATYPE_USERNAME")
    val password = System.getenv("SONATYPE_PASSWORD")
    var releaseMode: String? = null
    var versionSuffix: String? = null
    var deployUrl: String? = null

    val snapshotDeployUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
    val releaseDeployUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"

    fun initialize(project: Project) {
        val releaseMode: String? by project
        val versionSuffix = when (releaseMode) {
            "RELEASE" -> ""
            else -> "-SNAPSHOT"
        }

        Deployment.releaseMode = releaseMode
        Deployment.versionSuffix = versionSuffix
        Deployment.deployUrl = when (releaseMode) {
            "RELEASE" -> Deployment.releaseDeployUrl
            else -> Deployment.snapshotDeployUrl
        }

        initializePublishing(project)
        initializeSigning(project)
    }

    private fun initializePublishing(project: Project) {
        project.version = Versions.marathon + versionSuffix

        project.plugins.apply("maven-publish")

        val javaPlugin = project.the(JavaPluginConvention::class)

        val sourcesJar by project.tasks.creating(org.gradle.api.tasks.bundling.Jar::class) {
            classifier = "sources"
            from(javaPlugin.sourceSets["main"].allSource)
        }
        val javadocJar by project.tasks.creating(org.gradle.api.tasks.bundling.Jar::class) {
            classifier = "javadoc"
            from(javaPlugin.docsDir)
            dependsOn("javadoc")
        }

        project.configure<PublishingExtension> {
            publications {
                create("default", MavenPublication::class.java) {
                    Deployment.customizePom(project, pom)
                    from(project.components["java"])
                    artifact(sourcesJar)
                    artifact(javadocJar)
                }
            }
            repositories {
                maven {
                    name = "Local"
                    setUrl("${project.rootDir}/build/repository")
                }
                maven {
                    name = "OSSHR"
                    credentials {
                        username = Deployment.user
                        password = Deployment.password
                    }
                    setUrl(Deployment.deployUrl)
                }
            }
        }
    }

    private fun initializeSigning(project: Project) {
        val passphrase = System.getenv("GPG_PASSPHRASE")
        passphrase?.let {
            project.plugins.apply("signing")

            val publishing = project.the(PublishingExtension::class)
            project.configure<SigningExtension> {
                sign(publishing.publications.getByName("default"))
            }

            project.extra.set("signing.keyId", "1131CBA5")
            project.extra.set("signing.password", passphrase)
            project.extra.set("signing.secretKeyRingFile", "${project.rootProject.rootDir}/.buildsystem/secring.gpg")
        }
    }

    fun customizePom(project: Project, pom: MavenPom?) {
        pom?.apply {
            name.set(project.name)
            url.set("https://github.com/Malinskiy/marathon")
            description.set("Android & iOS test runner")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("marathon-team")
                    name.set("Marathon team")
                    email.set("anton@malinskiy.com")
                }
            }

            scm {
                url.set("https://github.com/Malinskiy/marathon")
            }
        }
    }
}