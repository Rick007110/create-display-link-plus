@echo off
rem Starts the dev Minecraft client with Create and this addon. Double-click or run from cmd.
setlocal
cd /d "%~dp0"

rem Gradle 8.14 can't run on the system default Java 26, so use the Java 21 JDK Gradle provisioned
set "JAVA_HOME=%USERPROFILE%\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java 21 JDK not found at "%JAVA_HOME%".
    echo Install a Java 21 JDK and point JAVA_HOME in this file at it.
    pause
    exit /b 1
)

call gradlew.bat runClient
if errorlevel 1 pause
