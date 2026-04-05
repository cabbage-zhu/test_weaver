@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

REM test-weaver Windows 安装/升级脚本
REM
REM 用法:
REM   install.bat              安装到当前项目 .claude\skills\test-weaver\
REM   install.bat --global     安装到 %USERPROFILE%\.claude\skills\test-weaver\
REM   install.bat --upgrade    升级已有安装
REM   install.bat --uninstall  卸载

set "REPO_URL=https://gitee.com/ouyang-wenxuan/test_weaver.git"
set "SKILL_NAME=test-weaver"

if "%~1"=="--help" goto :usage
if "%~1"=="-h" goto :usage
if "%~1"=="--global" goto :global
if "%~1"=="--upgrade" goto :upgrade
if "%~1"=="--uninstall" goto :uninstall
if "%~1"=="" goto :local
echo 未知选项: %~1
goto :usage

:usage
echo 用法: install.bat [选项]
echo.
echo 选项:
echo   (无)          安装到当前项目 .claude\skills\%SKILL_NAME%\
echo   --global      安装到 %%USERPROFILE%%\.claude\skills\%SKILL_NAME%\
echo   --upgrade     升级已有安装
echo   --uninstall   卸载
echo   --help        显示帮助
exit /b 0

:local
for /f "delims=" %%i in ('git rev-parse --show-toplevel 2^>nul') do set "PROJECT_ROOT=%%i"
if not defined PROJECT_ROOT (
    echo 当前不在 git 仓库中。使用 --global 进行全局安装。
    exit /b 1
)
set "TARGET=%PROJECT_ROOT%\.claude\skills\%SKILL_NAME%"
goto :install

:global
set "TARGET=%USERPROFILE%\.claude\skills\%SKILL_NAME%"
goto :install

:install
if exist "%TARGET%" (
    echo 已安装在 %TARGET%，使用 --upgrade 升级。
    exit /b 1
)
mkdir "%TARGET%\.." 2>nul
git clone "%REPO_URL%" "%TARGET%"
if errorlevel 1 (
    echo 安装失败。
    exit /b 1
)
echo 安装完成: %TARGET%
echo 在 Claude Code 中使用 /test-weaver 即可。
exit /b 0

:upgrade
set "EXISTING="
if exist ".claude\skills\%SKILL_NAME%" (
    set "EXISTING=.claude\skills\%SKILL_NAME%"
)
if not defined EXISTING (
    if exist "%USERPROFILE%\.claude\skills\%SKILL_NAME%" (
        set "EXISTING=%USERPROFILE%\.claude\skills\%SKILL_NAME%"
    )
)
if not defined EXISTING (
    echo 未找到已有安装。请先运行 install.bat 安装。
    exit /b 1
)
echo 升级: !EXISTING!
pushd "!EXISTING!"
for /f "tokens=2" %%v in ('findstr /b "version:" SKILL.md 2^>nul') do set "OLD_VER=%%v"
git pull origin master
for /f "tokens=2" %%v in ('findstr /b "version:" SKILL.md 2^>nul') do set "NEW_VER=%%v"
popd
echo 升级完成: %OLD_VER% -^> %NEW_VER%
exit /b 0

:uninstall
set "EXISTING="
if exist ".claude\skills\%SKILL_NAME%" (
    set "EXISTING=.claude\skills\%SKILL_NAME%"
)
if not defined EXISTING (
    if exist "%USERPROFILE%\.claude\skills\%SKILL_NAME%" (
        set "EXISTING=%USERPROFILE%\.claude\skills\%SKILL_NAME%"
    )
)
if not defined EXISTING (
    echo 未找到已有安装。
    exit /b 0
)
echo 即将删除: !EXISTING!
set /p "CONFIRM=确认? [y/N] "
if /i "!CONFIRM!"=="y" (
    rmdir /s /q "!EXISTING!"
    echo 已卸载。
) else (
    echo 取消。
)
exit /b 0
