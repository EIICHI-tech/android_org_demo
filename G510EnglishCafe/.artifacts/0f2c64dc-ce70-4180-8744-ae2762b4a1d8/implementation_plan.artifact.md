# Fix Inconsistent JVM-target compatibility

The build is failing because the Java compiler is targeting JVM 1.8 while the Kotlin compiler is targeting JVM 21. This inconsistency causes build failures in modern Android projects.

## Proposed Changes

### [app](file:///C:/G510/android_org_demo/G510EnglishCafe/app/build.gradle.kts)

#### [MODIFY] [build.gradle.kts](file:///C:/G510/android_org_demo/G510EnglishCafe/app/build.gradle.kts)

I will add the `compileOptions` and `kotlinOptions` blocks to the `android` section to explicitly set the JVM target to 21 for both Java and Kotlin.

```kotlin
android {
    ...
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}
```

## Verification Plan

### Automated Tests
- I will run `./gradlew assembleDebug` (or the equivalent Gradle task) to verify that the project builds successfully without the JVM target compatibility error.
