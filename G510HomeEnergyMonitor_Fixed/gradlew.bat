@echo off
setlocal
set "DIST=gradle-8.9-bin.zip"
set "BASE=%USERPROFILE%\.gradle\manual-wrapper"
set "ZIP=%BASE%\%DIST%"
set "HOME=%BASE%\gradle-8.9"
if not exist "%HOME%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  echo Downloading Gradle 8.9...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile '%ZIP%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%BASE%'"
  if errorlevel 1 exit /b 1
)
call "%HOME%\bin\gradle.bat" %*
