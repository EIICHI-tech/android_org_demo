# Fix JVM Target Inconsistency

The project is failing to build due to a mismatch between the Java compiler target (1.8) and the Kotlin compiler target (21). This plan will align both targets to JVM 21, which is recommended for modern Android development with AGP 8.x and Kotlin 2.0.

## User Review Required

> [!IMPORTANT]
> I will be setting the JVM target to **21**. This matches the version that the Kotlin compiler is currently attempting to use. Please ensure you have JDK 21 installed and configured in your Android Studio Gradle settings.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/G510/android_org_demo/WorldPhotoJourney_Portrait/app/build.gradle.kts)
- Add `compileOptions` to set `sourceCompatibility` and `targetCompatibility` to `JavaVersion.VERSION_21`.
- Add `kotlinOptions` to set `jvmTarget` to `"21"`.

## Verification Plan

### Automated Tests
- Run `gradlew :app:compileDebugKotlin` and `gradlew :app:compileDebugJavaWithJavac` to ensure they now use consistent targets and compile successfully.
- Run a full build using `gradlew assembleDebug`.

### Manual Verification
- Sync the project in Android Studio to ensure no IDE errors.
