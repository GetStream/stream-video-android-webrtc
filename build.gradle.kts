plugins {
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("signing")
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

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

val androidSourcesJar = tasks.register<Jar>("androidSourcesJar") {
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    archiveBaseName.set("stream-video-webrtc-android")
    archiveClassifier.set("sources")
    // This project only contains pre-built AAR artifacts, no source code
    // Create an empty sources jar to satisfy Maven Central requirements
    from(file("artifacts/"))
    includeEmptyDirs = false
}

private fun Provider<Jar>.archivePath() = flatMap(Jar::getArchiveFile).get().asFile.absolutePath

val javadocJar = tasks.register<Jar>("javadocJar") {
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    archiveBaseName.set("stream-video-webrtc-android")
    archiveClassifier.set("javadoc")
}

private val isSnapshot = project.findProperty("SNAPSHOT")?.toString()?.toBoolean() == true

// Where to publish: "streamRepo", "central", or both, comma-separated. CI always passes it;
// with nothing passed this stays on Central so a local publishToMavenLocal behaves as before.
// streamRepo stages a Maven-2 tree under build/staged-repo for a separate upload job.
private val publishTargets = providers.gradleProperty("streamPublishTargets")
    .getOrElse("central")
    .split(",")
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toSet()

require(publishTargets.isNotEmpty() && (publishTargets - setOf("central", "streamRepo")).isEmpty()) {
    "'streamPublishTargets' must be a comma-separated subset of central, streamRepo but was '$publishTargets'"
}

if (isSnapshot) {
    version = "${project.version}-SNAPSHOT"
}

tasks.register("printAllArtifacts") {
    dependsOn(androidSourcesJar, javadocJar)

    println("📦 Artifacts that will be published:")
    println("   Group ID: io.getstream")
    println("   Artifact ID: stream-video-webrtc-android")
    println("   Version: ${project.version}")
    println("")
    println("   Main AAR: ${aarFile.absolutePath} (${aarFile.length()} bytes)")
    println("   Sources JAR: ${androidSourcesJar.archivePath()}")
    println("   Javadoc JAR: ${javadocJar.archivePath()}")
    println("")
    println("   Repositories: ${publishTargets.joinToString()}")
    println("   Signing: ${if (isSnapshot) "Disabled (SNAPSHOT)" else "Required"}")
}

mavenPublishing {
    if ("central" in publishTargets) {
        publishToMavenCentral(automaticRelease = !isSnapshot)
    }

    coordinates(
        groupId = "io.getstream",
        artifactId = "stream-video-webrtc-android",
        version = project.version.toString()
    )

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

// Manually create the publication to include the pre-built AAR because that's not configurable
// through the maven.publish plugin.
if ("streamRepo" in publishTargets) {
    publishing {
        repositories {
            maven {
                name = "streamRepoStaging"
                url = layout.buildDirectory.dir("staged-repo").get().asFile.toURI()
            }
        }
    }
}

afterEvaluate {
    publishing {
        publications.create<MavenPublication>("prebuitltAar") {
            // Add pre-built AAR and the sources/javadoc jars
            artifact(aarFile)
            artifact(androidSourcesJar)
            artifact(javadocJar)
        }
    }

    // Configure signing conditionally using standard Gradle signing plugin
    if (!isSnapshot) {
        signing {
            sign(publishing.publications)
        }
    }
}

tasks.named("publish") {
    dependsOn("verifyAar")
}
