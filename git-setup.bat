@echo off
setlocal

REM =====================================================================
REM GitHub setup helper for Domestia Player Choice.
REM
REM This initializes Git if needed, switches the local branch to main,
REM and points origin to the correct GitHub repository.
REM =====================================================================

set "GIT_EXE=C:\Program Files\Git\cmd\git.exe"
set "REPOSITORY_URL=https://github.com/domestia-mods/domestia-player-choice.git"
set "BRANCH_NAME=main"

if not exist "%GIT_EXE%" (
    echo.
    echo ERROR: Git executable was not found:
    echo %GIT_EXE%
    echo.
    echo Update GIT_EXE in this file to match your Git installation path.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Git executable
echo ============================================================
"%GIT_EXE%" --version

if not exist ".git" (
    echo.
    echo ============================================================
    echo Initializing local Git repository
    echo ============================================================
    "%GIT_EXE%" init
    if errorlevel 1 goto :git_error
)

echo.
echo ============================================================
echo Setting branch name
echo ============================================================
"%GIT_EXE%" branch -M %BRANCH_NAME%
if errorlevel 1 goto :git_error

echo.
echo ============================================================
echo Setting origin remote
echo Repository: %REPOSITORY_URL%
echo ============================================================
"%GIT_EXE%" remote get-url origin >nul 2>nul
if errorlevel 1 (
    "%GIT_EXE%" remote add origin %REPOSITORY_URL%
) else (
    "%GIT_EXE%" remote set-url origin %REPOSITORY_URL%
)
if errorlevel 1 goto :git_error

echo.
echo ============================================================
echo Current remotes
echo ============================================================
"%GIT_EXE%" remote -v
if errorlevel 1 goto :git_error

echo.
echo ============================================================
echo Done.
echo ============================================================
pause
exit /b 0

:git_error
echo.
echo ERROR: Git command failed.
pause
exit /b 1
