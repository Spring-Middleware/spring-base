@echo off
REM Build only the registry Docker image using the runtime Dockerfile (Windows batch)
REM Usage: build-registry-image.bat [--jar <path>] [--dockerfile <path>] [--tag <image:tag>] [--no-cache]

setlocal EnableDelayedExpansion

set "JAR=parent/registry/boot/target/registry-boot-1.5.0.jar"
set "DOCKERFILE=parent/registry/boot/Dockerfile.runtime"
set "TAG=spring-base_registry:latest"
set "NO_CACHE=0"

:parseArgs
if "%~1"=="" goto argsParsed
if "%~1"=="--jar" (
  set "JAR=%~2" & shift & shift & goto parseArgs
)
if "%~1"=="--dockerfile" (
  set "DOCKERFILE=%~2" & shift & shift & goto parseArgs
)
if "%~1"=="--tag" (
  set "TAG=%~2" & shift & shift & goto parseArgs
)
if "%~1"=="--no-cache" (
  set "NO_CACHE=1" & shift & goto parseArgs
)
if "%~1"=="-h" if "%~2"=="" (echo Usage: %~nx0 [--jar <path>] [--dockerfile <path>] [--tag <image:tag>] [--no-cache] & goto end)

echo Unknown arg: %~1
goto end

:argsParsed
REM Resolve paths
set "SCRIPT_DIR=%~dp0"
REM remove trailing backslash
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
for %%I in ("%SCRIPT_DIR%\..") do set "REPO_ROOT=%%~fI"
set "DOCKERFILE_PATH=%REPO_ROOT%\%DOCKERFILE%"
set "JAR_PATH=%REPO_ROOT%\%JAR%"

echo Repository root: %REPO_ROOT%
echo Using Dockerfile: %DOCKERFILE_PATH%
echo Using JAR: %JAR_PATH%
echo Target image tag: %TAG%

where docker >nul 2>&1
if errorlevel 1 (
  echo Docker CLI not found in PATH. Please install Docker.
  goto end
)

if not exist "%DOCKERFILE_PATH%" (
  echo Dockerfile not found: %DOCKERFILE_PATH%
  goto end
)

if not exist "%JAR_PATH%" (
  echo Warning: JAR not found at %JAR_PATH%
  echo You can build it with: mvn -pl parent/registry/boot -am -DskipTests package
)

set "NO_CACHE_ARG="
if "%NO_CACHE%"=="1" set "NO_CACHE_ARG=--no-cache"

REM Change to repo root to avoid dockerfile path resolution issues on Windows
pushd "%REPO_ROOT%"

REM Build using '.' as context and the Dockerfile path relative to repo root
setlocal DisableDelayedExpansion
set "CMD=docker build %NO_CACHE_ARG% -f "%DOCKERFILE%" -t "%TAG%" --build-arg JAR="%JAR%" ."
endlocal & set "CMD=%CMD%"

echo Running: %CMD%
%CMD%
if errorlevel 1 (
  echo docker build failed with exit code %ERRORLEVEL%
  popd
  goto end
)

popd

echo Docker image built: %TAG%

:end
endlocal
exit /b
