# Implementation Plan - Fix JVM Target Inconsistency

The project build is failing due to a mismatch between Java (1.8) and Kotlin (21) JVM targets. This plan aligns both to use a consistent JVM version (Java 17) to ensure build compatibility and modern language features.

## Proposed Changes

### [Component: Build Configuration]

#### [MODIFY] [app/build.gradle.kts](file:///C:/G510/android_org_demo/EArmG510Controller/app/build.gradle.kts)
- Add `compileOptions` block to set `sourceCompatibility` and `targetCompatibility` to `JavaVersion.VERSION_17`.
- Add `kotlinOptions` block to set `jvmTarget` to `"17"`.
- *Alternative*: Configure a JVM toolchain for Kotlin, which automatically sets the Java compiler version as well.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify the build succeeds.
- Run `./gradlew check` to ensure all tests still pass with the new JVM target.

### Manual Verification
- Sync the project in Android Studio to ensure the IDE recognizes the changes.
