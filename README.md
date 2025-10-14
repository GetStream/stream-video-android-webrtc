# Stream Video Android WebRTC

This repository builds and publishes WebRTC Android AAR artifacts to Maven Central for easy consumption in Android applications. 
The [Stream video SDK](https://github.com/GetStream/stream-video-android) also uses the this artifact

The WebRTC source code comes from the [getStream/webrtc](https://github.com/GetStream/webrtc) repository, which is built using the `build-webrtc` workflow and the resulting AAR file is placed in this repository for publishing.

## GitHub Actions Workflow

There are two main workflows in the project - `build-webrtc.yml` and `publish-webrtc.yml`
- The `build-webrtc.yml` workflow provides an option to build from the a given branch of [webrtc](https://github.com/GetStream/webrtc) repo. 

- The `publish-webrtc.yml` workflow handles the publishing part of the artifact

## Artifact Storage

The compiled WebRTC AAR artifacts are stored in the `artifacts/` directory:

```
artifacts/
└── libwebrtc.aar
```

The AAR file must be placed in this directory before publishing can occur.

## Maven Dependency

To use the compiled WebRTC library in your Android application, add the following dependency to your `build.gradle` file:

### For Release Versions

```gradle
dependencies {
    implementation 'io.getstream:stream-video-webrtc-android:137.0.1'
}
```

### For Snapshot Versions (Development)

```gradle
repositories {
    maven { url 'https://central.sonatype.com/repository/maven-snapshots/' }
}

dependencies {
    implementation 'io.getstream:stream-video-webrtc-android:137.0.1-SNAPSHOT'
}
```

### Dependency Details

- **Group ID**: `io.getstream`
- **Artifact ID**: `stream-video-webrtc-android`
- **Repository**: Maven Central (central.sonatype.com)
- **Latest Version**: Check the [releases page](https://github.com/GetStream/stream-video-android-webrtc/releases) for the current version

## Publishing Process


## License

This project is licensed under the BSD 3-Clause License - see the [LICENSE](LICENSE) file for details.
