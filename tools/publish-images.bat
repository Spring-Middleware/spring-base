@echo off
REM Build and publish Docker images for registry and graphql-gateway (Windows batch)
REM Usage: publish-images.bat <version>
REM Example: publish-images.bat 1.0.0

setlocal EnableDelayedExpansion

if "%~1"=="" (
  echo Error: Please provide a version tag.
  echo Usage: %~nx0 ^<version^>
  echo Example: %~nx0 1.0.0
  goto end
)

set "VERSION=%~1"

REM Resolve paths
set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
for %%I in ("%SCRIPT_DIR%\..") do set "REPO_ROOT=%%~fI"

echo Repository root: %REPO_ROOT%
echo Target Version: %VERSION%

where docker >nul 2>&1
if errorlevel 1 (
  echo Docker CLI not found in PATH. Please install Docker.
  goto end
)

pushd "%REPO_ROOT%\tools"

echo ========================================================
echo Building Registry Image...
echo ========================================================
call build-registry-image.bat
if errorlevel 1 (
  echo Failed to build registry image.
  popd
  goto end
)

echo ========================================================
echo Building GraphQL Gateway Image...
echo ========================================================
call build-graphql-gateway-image.bat
if errorlevel 1 (
  echo Failed to build graphql-gateway image.
  popd
  goto end
)

echo ========================================================
echo Tagging Images...
echo ========================================================
docker tag spring-base_registry:latest ferguardiola/registry:%VERSION%
docker tag spring-base_graphql-gateway:latest ferguardiola/graphql-gateway:%VERSION%

echo ========================================================
echo Pushing Registry Image...
echo ========================================================
docker push ferguardiola/registry:%VERSION%
if errorlevel 1 (
  echo Failed to push registry image.
  popd
  goto end
)

echo ========================================================
echo Pushing GraphQL Gateway Image...
echo ========================================================
docker push ferguardiola/graphql-gateway:%VERSION%
if errorlevel 1 (
  echo Failed to push graphql-gateway image.
  popd
  goto end
)

popd

echo ========================================================
echo Successfully published images with tag: %VERSION%
echo   - ferguardiola/registry:%VERSION%
echo   - ferguardiola/graphql-gateway:%VERSION%
echo ========================================================

:end
endlocal
exit /b

