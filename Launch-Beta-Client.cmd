@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title Progression Path Companion - Beta Client
echo.
echo ============================================================
echo   Progression Path Companion - RuneLite Beta Client
echo ============================================================
echo.

if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat was not found next to this launcher.
    echo Extract or clone the complete companion repository, then try again.
    goto :failed
)

set "JAVA_VERSION="
set "JAVA_MAJOR="
for /f "tokens=3" %%V in ('call gradlew.bat --version 2^>^&1 ^| findstr /b /c:"Launcher JVM:"') do if not defined JAVA_VERSION set "JAVA_VERSION=%%~V"
for /f "tokens=1 delims=." %%M in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%M"

if not "%JAVA_MAJOR%"=="11" (
    echo ERROR: This beta project requires Java 11, but Gradle found Java %JAVA_VERSION%.
    echo Install or select Eclipse Temurin JDK 11 and try again.
    echo https://adoptium.net/temurin/releases/?version=11
    goto :failed
)

echo Java 11 found.
if /i "%~1"=="--check" (
    echo Beta client prerequisites passed.
    exit /b 0
)

echo Starting RuneLite with the local Progression Path Companion source...
echo Keep this window open while the development client is running.
echo.

call "%~dp0gradlew.bat" --no-daemon run
if errorlevel 1 goto :gradle_failed

echo.
echo RuneLite closed normally.
goto :done

:gradle_failed
echo.
echo ERROR: RuneLite did not start or exited with an error.
echo Read the messages above, then see BETA_TESTING.md for troubleshooting.
goto :failed

:failed
echo.
pause
exit /b 1

:done
echo.
pause
exit /b 0
