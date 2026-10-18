@echo off
setlocal
set V=8.9
set D=%USERPROFILE%\.gradle\wrapper\dists\gradle-%V%-bin\manual
set G=%D%\gradle-%V%in\gradle.bat
if not exist "%G%" (
  if not exist "%D%" mkdir "%D%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%V%-bin.zip' -OutFile '%D%\gradle.zip'; Expand-Archive -Force '%D%\gradle.zip' '%D%'; Remove-Item '%D%\gradle.zip'"
)
call "%G%" %*
