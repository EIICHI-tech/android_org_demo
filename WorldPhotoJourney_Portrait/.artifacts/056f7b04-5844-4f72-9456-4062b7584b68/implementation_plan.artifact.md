# Fix AAR Metadata Build Errors

The build is failing due to dependencies that require a higher `compileSdk` (API 37) and a higher Android Gradle Plugin version (AGP 9.1.0) than what is currently configured.

The current project uses:
- `compileSdk = 35`
- `AGP = 8.13.2`

## User Review Required

> [!IMPORTANT]
> To fix the build, I will downgrade several dependencies to versions that are compatible with your current SDK and AGP setup. If you specifically need the latest features from these libraries, we would instead need to upgrade the entire toolchain (SDK and AGP), which is a larger change.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/G510/android_org_demo/WorldPhotoJourney_Portrait/app/build.gradle.kts)
- Downgrade `com.google.maps.android:maps-compose` from `8.4.0` to `6.4.0`.
- Downgrade `androidx.activity:activity-compose` from `1.10.0` to `1.9.3` to avoid pulling in bleeding-edge `androidx.core` versions.
- (If necessary) Explicitly constrain `androidx.core:core-ktx` to a compatible version like `1.15.0`.

## Verification Plan

### Automated Tests
- Run `gradle_build(":app:assembleDebug")` to verify that the AAR metadata check passes and the project compiles.
