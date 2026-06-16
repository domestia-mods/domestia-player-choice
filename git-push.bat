@echo off
setlocal

REM =====================================================================
REM Git helper: stage, commit, and push local changes.
REM
REM Usage:
REM   git-push.bat
REM   git-push.bat "Update README layout"
REM
REM This script is configured for:
REM   https://github.com/domestia-mods/domestia-player-choice.git
REM =====================================================================

set "GIT_EXE=C:\Program Files\Git\cmd\git.exe"
set "REPOSITORY_URL=https://github.com/domestia-mods/domestia-player-choice.git"
set "BRANCH_NAME=main"

set "COMMIT_MESSAGE=%~1"
if "%COMMIT_MESSAGE%"=="" (
    set "COMMIT_MESSAGE=Update project files"
)

if not exist "%GIT_EXE%" (
    echo.
    echo ERROR: Git executable was not found:
    echo %GIT_EXE%
    echo.
    echo Update GIT_EXE in this file to match your Git installation path.
    pause
    exit /b 1
)

if not exist ".git" (
    echo.
    echo ERROR: This folder is not a Git repository yet.
    echo Run git-setup.bat first.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Git executable
echo ============================================================
"%GIT_EXE%" --version

call :ensure_remote
if errorlevel 1 exit /b 1

echo.
echo ============================================================
echo Git status before staging
echo ============================================================
"%GIT_EXE%" status
if errorlevel 1 goto :git_error

echo.
echo ============================================================
echo Staging all changes
echo ============================================================
"%GIT_EXE%" add .
if errorlevel 1 goto :git_error

echo.
echo ============================================================
echo Git status after staging
echo ============================================================
"%GIT_EXE%" status
if errorlevel 1 goto :git_error

echo.
echo ============================================================
echo Creating commit
echo Message: %COMMIT_MESSAGE%
echo ============================================================
"%GIT_EXE%" commit -m "%COMMIT_MESSAGE%"
if errorlevel 1 (
    echo.
    echo Commit was not created.
    echo This usually means there are no staged changes, or Git reported an error.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Pushing to origin/%BRANCH_NAME%
echo ============================================================
"%GIT_EXE%" push -u origin %BRANCH_NAME%
if errorlevel 1 goto :git_error

echo.
echo ============================================================
echo Done.
echo ============================================================
pause
exit /b 0

:ensure_remote
"%GIT_EXE%" branch -M %BRANCH_NAME%
if errorlevel 1 exit /b 1
"%GIT_EXE%" remote get-url origin >nul 2>nul
if errorlevel 1 (
    "%GIT_EXE%" remote add origin %REPOSITORY_URL%
) else (
    "%GIT_EXE%" remote set-url origin %REPOSITORY_URL%
)
if errorlevel 1 exit /b 1
exit /b 0

:git_error
echo.
echo ERROR: Git command failed.
pause
exit /b 1
