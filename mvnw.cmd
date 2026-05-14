@REM Maven Wrapper script for Windows
@REM Downloads and runs Maven automatically

@echo off
setlocal

set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6"

if not exist "%WRAPPER_JAR%" (
    echo [ERROR] Wrapper JAR not found: %WRAPPER_JAR%
    pause
    exit /b 1
)

@REM Use wrapper JAR to download and run Maven
java -jar "%WRAPPER_JAR%" "-Dmaven.home=%MAVEN_HOME%" %*
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Maven execution failed.
    pause
)
