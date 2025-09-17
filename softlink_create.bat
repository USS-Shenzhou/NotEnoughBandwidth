@echo off
:: 判断是否有参数（拖拽的文件路径）
if "%~1"=="" (
    echo 请把文件拖拽到本bat上来使用。
    pause
    exit /b
)

:: 获取原文件的完整路径
set "src=%~1"

:: 获取原文件的文件名
set "name=%~nx1"

:: 获取当前bat所在目录
set "batdir=%~dp0"

:: 目标链接路径（放在bat所在目录）
set "link=%batdir%%name%"

:: 判断目标是否已存在
if exist "%link%" (
    echo 目标 "%link%" 已存在，无法创建。
    pause
    exit /b
)

:: 判断是文件还是文件夹
if exist "%src%\*" (
    echo 创建文件夹链接...
    mklink /D "%link%" "%src%"
) else (
    echo 创建文件链接...
    mklink "%link%" "%src%"
)

pause
