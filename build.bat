@echo off
setlocal enabledelayedexpansion

set "GRADLE_VERSION=9.4.1"
set "JDK_MAJOR=25"
set "ROOT=%~dp0"
set "TOOLS=%ROOT%.gradle-local"
set "JDK_TOOLS=%ROOT%.jdk-local"
set "GRADLE_HOME=%TOOLS%\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%TOOLS%\gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
set "JDK_ZIP=%JDK_TOOLS%\jdk-%JDK_MAJOR%-windows-x64.zip"
set "JDK_URL=https://api.adoptium.net/v3/binary/latest/%JDK_MAJOR%/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
set "GRADLE_USER_HOME=%ROOT%.gradle-cache"
set "JAVA_HOME_FILE=%JDK_TOOLS%\java-home.txt"

cd /d "%ROOT%"

if not exist "%TOOLS%" mkdir "%TOOLS%"
if not exist "%JDK_TOOLS%" mkdir "%JDK_TOOLS%"

if not exist "%JAVA_HOME_FILE%" (
    echo Downloading JDK %JDK_MAJOR%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$ErrorActionPreference='Stop';" ^
        "Invoke-WebRequest -Uri '%JDK_URL%' -OutFile '%JDK_ZIP%';" ^
        "Expand-Archive -Path '%JDK_ZIP%' -DestinationPath '%JDK_TOOLS%' -Force;" ^
        "$java = Get-ChildItem -Path '%JDK_TOOLS%' -Recurse -Filter java.exe | Where-Object { $_.FullName -like '*\bin\java.exe' } | Select-Object -First 1;" ^
        "if (-not $java) { throw 'Unable to find java.exe in downloaded JDK'; }" ^
        "$jdkHome = Split-Path -Parent (Split-Path -Parent $java.FullName);" ^
        "Set-Content -Path '%JAVA_HOME_FILE%' -Value $jdkHome -NoNewline"
    if errorlevel 1 (
        echo.
        echo Failed to download or extract JDK %JDK_MAJOR%.
        echo Please check your network connection, then run build.bat again.
        exit /b 1
    )
)

set /p JAVA_HOME=<"%JAVA_HOME_FILE%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$ErrorActionPreference='Stop';" ^
        "Invoke-WebRequest -Uri '%GRADLE_URL%' -OutFile '%GRADLE_ZIP%';" ^
        "Expand-Archive -Path '%GRADLE_ZIP%' -DestinationPath '%TOOLS%' -Force"
    if errorlevel 1 (
        echo.
        echo Failed to download or extract Gradle.
        echo Please check your network connection, then run build.bat again.
        exit /b 1
    )
)

echo Building SoundRecord jars...
echo Using Java from PATH:
java -version
call "%GRADLE_HOME%\bin\gradle.bat" --no-daemon clean buildJars
if errorlevel 1 (
    echo.
    echo Build failed.
    exit /b 1
)

echo.
echo Build complete.
echo Fabric mod:   %ROOT%build\dist\SoundRecord-Fabric-1.0.0.jar
echo Paper plugin: %ROOT%build\dist\SoundRecord-Paper-1.0.0.jar
