# Publishing WebRTC Android to Maven Central

This project has been converted to a Gradle project to streamline the publishing process to Maven Central.

## Project Structure

The project now includes:
- `build.gradle` - Main build configuration with Maven publishing setup
- `settings.gradle` - Project settings
- `gradle.properties` - Default configuration (secrets overridden in CI)
- `gradlew` & `gradle/wrapper/` - Gradle wrapper for consistent builds

## Publishing Configuration

### Maven Central Setup

The project is configured to publish to Maven Central with:
- **Group ID**: `io.getstream`
- **Artifact ID**: `webrtc-android`
- **Repository**: Sonatype Central (central.sonatype.com)

### Version Management

- **Snapshot versions**: End with `-SNAPSHOT` and are published to snapshots repository
- **Release versions**: Published to staging repository for manual release

### Signing

Release versions are automatically signed using GPG keys configured via GitHub secrets.

## GitHub Actions Workflow

The `publish-webrtc.yml` workflow handles the complete publishing process:

### Triggers

1. **Manual Dispatch**: Run manually with custom version and settings
2. **Automatic**: Runs after successful completion of the build workflow

### Required Secrets

Configure these secrets in your GitHub repository:

```
OSSRH_USERNAME          # Sonatype username
OSSRH_PASSWORD          # Sonatype password
SIGNING_KEY_ID          # GPG key ID
SIGNING_PASSWORD        # GPG key passphrase
SIGNING_KEY             # GPG private key (armored)
SONATYPE_STAGING_PROFILE_ID  # Sonatype staging profile ID
```

### Workflow Inputs (Manual Dispatch)

- `version`: Version to publish (e.g., "1.0.0" or "1.0.0-SNAPSHOT")
- `branch`: WebRTC branch to use (default: "patch/m137.5")
- `publish_snapshot`: Whether to publish as snapshot (default: true)

## Local Development

### Prerequisites

- Java 8 or higher
- The `libwebrtc.aar` file from the build workflow

### Available Tasks

```bash
# Verify the AAR file exists and is valid
./gradlew verifyAar

# Show all available tasks
./gradlew tasks

# Print all artifacts that will be published
./gradlew printAllArtifacts

# Publish to Maven Central (requires environment variables)
./gradlew publishToSonatype

# Publish and automatically release to Maven Central
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

### Configuration

The build script supports multiple configuration methods:

#### 1. Environment Variables (Recommended for CI)
```bash
export OSSRH_USERNAME=your-username
export OSSRH_PASSWORD=your-password
export SIGNING_KEY_ID=your-key-id
export SIGNING_PASSWORD=your-passphrase
export SIGNING_KEY=your-private-key
export SONATYPE_STAGING_PROFILE_ID=your-profile-id
export SNAPSHOT=true
export version=1.0.0-SNAPSHOT
```

#### 2. Local Properties File (for local development)
Create a `local.properties` file in the project root:
```properties
ossrhUsername=your-username
ossrhPassword=your-password
signing.keyId=your-key-id
signing.password=your-passphrase
signing.key=your-private-key
sonatypeStagingProfileId=your-profile-id
snapshot=true
version=1.0.0-SNAPSHOT
```

#### 3. Gradle Properties (fallback)
The `gradle.properties` file contains default values that can be overridden.

## Publishing Process

### For Snapshot Versions

1. Run the publish workflow with `publish_snapshot: true`
2. Artifacts are immediately available at:
   `https://central.sonatype.com/repository/maven-snapshots/io/getstream/webrtc-android/`

### For Release Versions

1. Run the publish workflow with `publish_snapshot: false` and a release version (e.g., "1.0.0")
2. The artifact is automatically published to the staging repository, closed, and released
3. The artifact will be available on Maven Central within 10 minutes

## Usage in Android Projects

Once published, you can use the library in your Android project:

```gradle
dependencies {
    implementation 'io.getstream:webrtc-android:1.0.0'
}
```

For snapshot versions:
```gradle
repositories {
    maven { url 'https://central.sonatype.com/repository/maven-snapshots/' }
}

dependencies {
    implementation 'io.getstream:webrtc-android:1.0.0-SNAPSHOT'
}
```

## Troubleshooting

### Common Issues

1. **AAR file not found**: Ensure the build workflow has completed successfully and the artifact is available
2. **Authentication errors**: Verify all required secrets are configured correctly
3. **Signing errors**: Ensure GPG keys are properly configured and accessible
4. **Version conflicts**: Check that the version doesn't already exist in the repository

### Debugging

Enable debug output by adding to `gradle.properties`:
```properties
org.gradle.logging.level=debug
```

Or run with debug flag:
```bash
./gradlew publish --debug
```
