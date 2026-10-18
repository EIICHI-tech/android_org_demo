# Walkthrough - Fixing Internal Compiler Error (ICE)

I have successfully resolved the `Internal compiler error: source must not be null` that was preventing the project from building.

## Changes Made

### Compiler Configuration
- **Upgraded Kotlin version**: Updated `org.jetbrains.kotlin.android` and `org.jetbrains.kotlin.plugin.compose` from `2.0.21` to `2.1.0`. The crash was specifically related to the K2 compiler's diagnostic reporting in version 2.0.21.

### Code Refactoring
- **[EArmUsbClient.kt](file:///C:/G510/android_org_demo/EArmG510UsbController/app/src/main/java/jp/co/kagafei/earmg510/EArmUsbClient.kt)**:
    - Replaced `runCatching` with standard `try-catch` blocks.
    - Replaced `error()` calls with explicit `throw IllegalStateException()`.
    - Added explicit return types to all functions.
    - Improved code readability by expanding one-liners.
    - Restored the `request` function.

- **[EArmViewModel.kt](file:///C:/G510/android_org_demo/EArmG510UsbController/app/src/main/java/jp/co/kagafei/earmg510/EArmViewModel.kt)**:
    - Expanded compact one-liners into more explicit function bodies.
    - Added explicit types for `StateFlow` properties.
    - Replaced `Result.onSuccess`/`onFailure` chains with explicit `if (result.isSuccess)` checks to simplify type inference for the compiler.

## Verification Results

### Automated Tests
- Ran `./gradlew :app:compileDebugKotlin` and the build finished **successfully**.

> [!NOTE]
> The ICE was likely triggered by a combination of Kotlin 2.0.21's K2 compiler, very compact code structures, and complex type inference involving `Result` and `Nothing` (from `error()`). The upgrade to 2.1.0 combined with more explicit code structure provides a robust fix.
