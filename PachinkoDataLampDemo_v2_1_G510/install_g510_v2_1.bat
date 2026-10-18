@echo off
set PACKAGE=jp.co.demo.pachinkodatalamp
set APK=app\build\outputs\apk\debug\app-debug.apk
adb devices
if not exist "%APK%" (
  echo APK not found. Build debug APK in Android Studio first.
  pause
  exit /b 1
)
adb uninstall %PACKAGE%
adb install "%APK%"
adb shell am start -n %PACKAGE%/.MainActivity
echo.
echo Install completed. Press HOME and open the app drawer.
pause
