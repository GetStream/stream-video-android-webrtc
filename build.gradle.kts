import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.dokka") version "1.9.10"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

extra["ossrhUsername"] = System.getenv("OSSRH_USERNAME") ?: ""
extra["ossrhPassword"] = System.getenv("OSSRH_PASSWORD") ?: ""
extra["sonatypeStagingProfileId"] = System.getenv("SONATYPE_STAGING_PROFILE_ID") ?: ""
extra["signing.keyId"] = System.getenv("SIGNING_KEY_ID") ?: ""
extra["signing.password"] = System.getenv("SIGNING_PASSWORD") ?: ""
extra["signing.key"] = System.getenv("SIGNING_KEY") ?: ""

val aarFile = file("artifacts/libwebrtc.aar")

tasks.register("verifyAar") {
    doLast {
        if (!aarFile.exists()) {
            throw GradleException("libwebrtc.aar not found in artifacts/ directory.")
        }
        println("✅ AAR file verified: ${aarFile.absolutePath}")
        println("   Size: ${aarFile.length()} bytes")
    }
}

tasks.register("printAllArtifacts") {
    doLast {
        println("📦 Artifacts that will be published:")
        println("   Group ID: io.getstream")
        println("   Artifact ID: stream-video-webrtc-android")
        println("   Version: ${project.version}")
        println("")
        println("   Main AAR: ${aarFile.absolutePath} (${aarFile.length()} bytes)")
        println("   Sources JAR: ${tasks.named<Jar>("androidSourcesJar").get().archiveFile.get().asFile.absolutePath}")
        println("   Javadoc JAR: ${tasks.named<Jar>("javadocJar").get().archiveFile.get().asFile.absolutePath}")
        println("")
        println("   Repository: Maven Central (via Sonatype)")
        println("   Signing: ${if (project.version.toString().endsWith("-SNAPSHOT")) "Disabled (SNAPSHOT)" else "Required"}")
    }
}

if (project.hasProperty("version")) {
    project.version = project.property("version")!!
} else {
    project.version = "LOCAL-DEV" // fallback for local builds
}

tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    // This project only contains pre-built AAR artifacts, no source code
    // Create an empty sources jar to satisfy Maven Central requirements
    from(file("artifacts/"))
    includeEmptyDirs = false
}

tasks.withType<DokkaTask>().configureEach {
    pluginsMapConfiguration.set(
        mapOf("org.jetbrains.dokka.base.DokkaBase" to """{ "separateInheritedMembers": true}""")
    )
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml.get().outputDirectory)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "io.getstream"
                artifactId = "stream-video-webrtc-android"
                version = project.version.toString()
                artifact(aarFile)
                artifact(tasks.named("androidSourcesJar"))
                artifact(tasks.named("javadocJar"))

                pom {
                    name.set("WebRTC Android")
                    description.set("WebRTC library for Android")
                    url.set("https://github.com/GetStream/stream-video-android-webrtc")
                    licenses {
                        license {
                            name.set("The BSD 3-Clause License")
                            url.set("https://opensource.org/licenses/BSD-3-Clause")
                        }
                    }
                    developers {
                        developer {
                            id.set("getstream")
                            name.set("GetStream")
                            email.set("support@getstream.io")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/GetStream/stream-video-android-webrtc.git")
                        developerConnection.set("scm:git:ssh://github.com:GetStream/stream-video-android-webrtc.git")
                        url.set("https://github.com/GetStream/stream-video-android-webrtc")
                    }
                }
            }
        }
    }

    signing {
        setRequired { !project.version.toString().endsWith("-SNAPSHOT") }

        val keyId = rootProject.extra["signing.keyId"] as String
        val key = rootProject.extra["signing.key"] as String
        val pass = rootProject.extra["signing.password"] as String

        useInMemoryPgpKeys(keyId, key, pass)
        sign(publishing.publications)
    }

    // Validate signing credentials only when signing task is executed
    tasks.withType<Sign>().configureEach {
        doFirst {
            if (!project.version.toString().endsWith("-SNAPSHOT")) {
                val keyId = rootProject.extra["signing.keyId"] as String
                val key = rootProject.extra["signing.key"] as String
                val pass = rootProject.extra["signing.password"] as String

                if (key.isEmpty() || pass.isEmpty()) {
                    throw GradleException("❌ Missing signing credentials for release build!")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            stagingProfileId.set(rootProject.extra["sonatypeStagingProfileId"] as String)
            username.set(rootProject.extra["ossrhUsername"] as String)
            password.set(rootProject.extra["ossrhPassword"] as String)
        }
    }
}

tasks.named("publish") {
    dependsOn("verifyAar")
}
