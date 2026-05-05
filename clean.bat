@echo off
setlocal

set "GRADLE_VERSION=9.4.1"
set "ROOT=%~dp0"
set "GRADLE_HOME=%ROOT%.gradle-local\gradle-%GRADLE_VERSION%"
set "JAVA_HOME_FILE=%ROOT%.jdk-local\java-home.txt"
set "GRADLE_USER_HOME=%ROOT%.gradle-cache"

if exist "%JAVA_HOME_FILE%" (
    set /p JAVA_HOME=<"%JAVA_HOME_FILE%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    set "JAVA_HOME="
)

cd /d "%ROOT%"

if exist "%GRADLE_HOME%\bin\gradle.bat" (
    call "%GRADLE_HOME%\bin\gradle.bat" --no-daemon clean
) else (
    if exist build rmdir /s /q build
    if exist common\build rmdir /s /q common\build
    if exist fabric-mod\build rmdir /s /q fabric-mod\build
    if exist paper-plugin\build rmdir /s /q paper-plugin\build
)
