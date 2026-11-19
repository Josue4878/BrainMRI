@echo off
REM Windows bootstrap for Gradle 8.9 (uses verified official Gradle download).
REM No Android Studio or preinstalled Gradle required.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0gradle-bootstrap.ps1" %*
exit /b %errorlevel%
