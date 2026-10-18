@echo off
setlocal
set "BASE=%USERPROFILE%\.gradle\g510-amr-wrapper"
set "ZIP=%BASE%\gradle-8.9-bin.zip"
set "GHOME=%BASE%\gradle-8.9"
if not exist "%GHOME%\bin\gradle.bat" (
 if not exist "%BASE%" mkdir "%BASE%"
 powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile '%ZIP%'"
 if errorlevel 1 exit /b 1
 powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%BASE%'"
 if errorlevel 1 exit /b 1
)
call "%GHOME%\bin\gradle.bat" %*
